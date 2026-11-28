// Search engine: Arabic normalization + inverted index + BM25 with prefix matching.
// Runs in a Web Worker (and in Node for testing). It never changes the source text:
// it only indexes a normalized copy and returns record ids.
const MARKS = /[ؐ-ًؚ-ٰٟۖ-ۭ࣓-ࣿـ]/g;
function norm(t) {
  return t.replace(MARKS, '')
    .replace(/[ٱأإآ]/g, 'ا').replace(/ى/g, 'ي').replace(/ة/g, 'ه')
    .replace(/ؤ/g, 'و').replace(/ئ/g, 'ي').replace(/ء/g, '')
    .replace(/[^ء-ي\s]/g, ' ');
}
const PRE = ['وبال', 'فبال', 'وكال', 'وال', 'فال', 'بال', 'كال', 'لل', 'ال', 'و', 'ف', 'ب', 'ل'];
function stem(w) {
  for (const p of PRE) if (w.startsWith(p) && w.length - p.length >= 3) return w.slice(p.length);
  return w;
}
function tokens(t) { return norm(t).split(/\s+/).filter(w => w.length > 1).map(stem); }

// Very common chain/filler words that would drown out real matches.
const STOP = new Set(['حدثنا', 'حدثني', 'اخبرنا', 'اخبرني', 'عن', 'قال', 'قالت', 'قالوا', 'ابن', 'بن', 'ابي', 'ابو', 'الله', 'صلي', 'عليه', 'وسلم', 'رسول', 'النبي', 'رضي', 'عنه', 'عنها', 'في', 'من', 'علي', 'ان', 'لا', 'ما', 'هذا', 'ذلك', 'كان', 'التي', 'الذي', 'هو', 'هي', 'او', 'ثم', 'اذا', 'قد', 'لم', 'يا', 'سمعت', 'نحوه', 'بمثله', 'اسناد', 'بهذا', 'يعني', 'وهو', 'مثل', 'حديث']);

// Allowed endings after a search word (pronouns, plurals, duals, verb endings).
const SUF = new Set(['', 'ا', 'ه', 'ها', 'هم', 'هما', 'هن', 'ي', 'ك', 'كم', 'كما', 'نا', 'ت', 'تم', 'تما', 'ن', 'ون', 'ين', 'ان', 'ات', 'وا', 'وه', 'وها', 'وهم', 'تها', 'ته', 'اته', 'اتها', 'اتهم', 'اتكم', 'ني', 'يه', 'يها', 'يهم', 'يكم']);

// Darija / Arabizi in Latin letters -> pattern over Arabic letters (sabgh -> [سص]ب[ا]?غ).
const LAT = [['gh','غ'],['kh','خ'],['ch','ش'],['sh','ش'],['th','[ثت]'],['dh','[ذضد]'],['ou','و?'],['aa','ا'],['ee','ي'],['oo','و'],
  ['3','ع'],['7','ح'],['9','ق'],['5','خ'],['8','غ'],['2',''],['b','ب'],['t','[تط]'],['d','[دض]'],['r','ر'],['z','[زظ]'],['s','[سص]'],
  ['f','ف'],['q','ق'],['k','ك'],['l','ل'],['m','م'],['n','ن'],['h','[هح]'],['w','و'],['y','ي'],['j','ج'],['g','[جغك]'],['c','[سك]'],['x','[كس]'],['p','ب'],['v','ف'],
  ['a','ا?'],['e','[اي]?'],['i','ي?'],['o','و?'],['u','و?']];
function latinPattern(w) {
  w = w.toLowerCase().replace(/[^a-z0-9]/g, '').replace(/([bcdfghjklmnpqrstvwxz])\1/g, '$1');
  if (w.length > 4) w = w.replace(/^(al|el|l)(?=[^aeiou])/, '');
  if (w.length < 2) return null;
  let out = '', i = 0;
  while (i < w.length) {
    const m = LAT.find(([k]) => w.startsWith(k, i));
    if (!m) { i++; continue; }
    out += m[1]; i += m[0].length;
  }
  if (/[at]$/.test(w)) out += '?' === out.slice(-1) ? '' : '';
  if (/a$/.test(w)) out = out.replace(/ا\?$/, '[اه]?');
  if (/t$/.test(w)) out = out.replace(/\[تط\]$/, '[تطه]');
  if (out.replace(/[\[\]?]/g, '').length < 2) return null;
  return new RegExp('^' + out + '$');
}
class Engine {
  constructor() { this.docs = []; this.post = new Map(); this.len = []; this.vocab = null; }
  add(text) {
    const id = this.docs.length; this.docs.push(1);
    const tf = new Map(); const ts = tokens(text);
    for (const w of ts) tf.set(w, (tf.get(w) || 0) + 1);
    this.len.push(ts.length);
    for (const [w, c] of tf) { let p = this.post.get(w); if (!p) this.post.set(w, p = []); p.push(id, c); }
    return id;
  }
  finish() {
    this.vocab = [...this.post.keys()].sort();
    const n = this.len.length; this.avg = this.len.reduce((a, b) => a + b, 0) / n;
  }
  // terms: [{w, wt}] ; returns [[id, score, hits]] sorted
  expand(term) {
    if (/[a-z0-9]/i.test(term)) {
      const re = latinPattern(term); if (!re) return [];
      const hit = this.vocab.filter(v => re.test(v) && !STOP.has(v));
      const out = new Set(hit);
      for (const v of hit) if (v.length >= 3) for (const x of this.expandT(v)) out.add(x);
      return [...out];
    }
    return this.expandT(stem(norm(term).trim().replace(/\s+/g, '')));
  }
  expandExact(term) {
    const t = stem(norm(term).trim().replace(/\s+/g, ''));
    if (t.length < 2 || STOP.has(t)) return [];
    const out = [];
    let lo = 0, hi = this.vocab.length;
    while (lo < hi) { const m = (lo + hi) >> 1; if (this.vocab[m] < t) lo = m + 1; else hi = m; }
    for (let i = lo; i < this.vocab.length && this.vocab[i].startsWith(t); i++) if (SUF.has(this.vocab[i].slice(t.length))) out.push(this.vocab[i]);
    return out;
  }
  expandT(t) {
    if (t.length < 2) return [];
    if (STOP.has(t)) return [];
    if (t.length < 3) return this.post.has(t) ? [t] : [];
    // Match the word with verb prefixes (يصبغون، تصبغ، اصبغوا) and allowed endings.
    const out = new Set();
    // Verbal nouns -> their three-letter root: تخفيف→خفف، تيسير→يسر، اصلاح→صلح، استغفار→غفر
    const roots = [t];
    if (t.length === 5 && t[0] === 'ت' && t[3] === 'ي') roots.push(t[1] + t[2] + t[4]);
    if (t.length === 5 && t[0] === 'ا' && t[3] === 'ا') roots.push(t[1] + t[2] + t[4]);
    if (t.length === 7 && t.startsWith('است') && t[5] === 'ا') roots.push(t[3] + t[4] + t[6]);
    for (const r of roots) for (const vp of ['', 'ي', 'ت', 'ن', 'ا']) {
      const p = vp + r;
      let lo = 0, hi = this.vocab.length;
      while (lo < hi) { const m = (lo + hi) >> 1; if (this.vocab[m] < p) lo = m + 1; else hi = m; }
      for (let i = lo; i < this.vocab.length && this.vocab[i].startsWith(p); i++) {
        if (SUF.has(this.vocab[i].slice(p.length))) out.add(this.vocab[i]);
      }
    }
    return [...out];
  }
  search(terms, limit = 60) {
    const N = this.len.length, k1 = 1.2, b = 0.6;
    const score = new Map(), hits = new Map();
    terms.forEach(({ w, wt, x }, ti) => {
      for (const v of (x ? this.expandExact(w) : this.expand(w))) {
        const p = this.post.get(v); const df = p.length / 2;
        const idf = Math.log(1 + (N - df + 0.5) / (df + 0.5));
        for (let i = 0; i < p.length; i += 2) {
          const id = p[i], c = p[i + 1];
          const s = wt * idf * (c * (k1 + 1)) / (c + k1 * (1 - b + b * this.len[id] / this.avg));
          score.set(id, (score.get(id) || 0) + s);
          let h = hits.get(id); if (!h) hits.set(id, h = new Set()); if (wt >= 1) h.add(ti);
        }
      }
    });
    const res = [];
    for (const [id, s] of score) { const n = hits.get(id).size; res.push([id, s * (1 + 0.6 * Math.max(0, n - 1)), n]); }
    res.sort((a, b) => b[1] - a[1]);
    return res.slice(0, limit);
  }
}


// ---------- Topic dictionary: Darija / French / English / Arabic -> Arabic words found in the texts ----------
function latSkel(w) {
  return w.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '')
    .replace(/sh/g, 'ch').replace(/ou/g, 'u').replace(/[aeiouy'\-]/g, '').replace(/(.)\1+/g, '$1');
}
function keyOf(w) {
  if (/[a-z0-9]/i.test(w)) {
    let x = w.toLowerCase().replace(/[^a-z0-9]/g, '');
    if (x.length > 4) x = x.replace(/^(al|el|l)(?=[^aeiou])/, '');
    return 'L' + latSkel(x);
  }
  return 'A' + stem(norm(w).replace(/\s/g, ''));
}
class Topics {
  constructor(txt) {
    this.list = []; this.idx = new Map();
    for (const line of txt.split('\n')) {
      let [trig, words] = line.split('|'); if (!words) continue;
      const wt = trig.startsWith('~') ? 0.35 : 1; trig = trig.replace(/^~/, '');
      const t = { wt, trig: trig.split(',').map(s => s.trim()).filter(Boolean), words: words.split(',').map(s => s.trim()).filter(Boolean) };
      const id = this.list.push(t) - 1;
      for (const tr of t.trig) {
        const parts = tr.split(/\s+/).map(keyOf).filter(k => k.length > 2);
        if (!parts.length) continue;
        const k = parts.join(' ');
        if (!this.idx.has(k)) this.idx.set(k, new Set());
        this.idx.get(k).add(id);
      }
    }
  }
  match(text) {
    const ks = text.split(/[\s,.;:!?؟،]+/).filter(Boolean).map(keyOf).filter(k => k.length > 2);
    const hit = new Set();
    for (let n = 3; n >= 1; n--) for (let i = 0; i + n <= ks.length; i++) {
      const s = this.idx.get(ks.slice(i, i + n).join(' '));
      if (s) s.forEach(x => hit.add(x));
    }
    const words = new Map();
    for (const id of hit) for (const w of this.list[id].words) {
      const ps = w.split(/\s+/);
      for (const p of ps) if (ps.length === 1 || p.length > 4) words.set(p, Math.max(words.get(p) || 0, this.list[id].wt));
    }
    return { n: hit.size, words: [...words.keys()], weighted: [...words].map(([w, wt]) => ({ w, wt, x: true })) };
  }
}
if (typeof module !== 'undefined') module.exports = { Topics };
if (typeof module !== 'undefined') Object.assign(module.exports, { Engine, norm, tokens });
