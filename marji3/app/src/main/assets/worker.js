importScripts('search.js');
let TP = null, E = null, ref = [], texts = [];
async function load() {
  const files = ['data/quran.json', 'data/bukhari.json', 'data/muslim.json'];
  const out = [];
  for (let i = 0; i < files.length; i++) {
    const r = await fetch(files[i]);
    if (!r.ok) throw new Error('load ' + files[i]);
    out.push(await r.json());
    postMessage({ type: 'progress', done: i + 1, total: files.length + 1 });
  }
  const [Q, B, M] = out;
  try { TP = new Topics(await (await fetch('topics.txt')).text()); } catch (e) { TP = null; }
  E = new Engine();
  Q.q.forEach((s, si) => s.forEach((a, ai) => { E.add(a); ref.push(['q', si, ai]); }));
  B.forEach((h, i) => { E.add(h.t); ref.push(['b', i]); texts[ref.length - 1] = h.t; });
  M.forEach((h, i) => { E.add(h.t); ref.push(['m', i]); texts[ref.length - 1] = h.t; });
  E.finish();
  postMessage({ type: 'ready' });
}
onmessage = e => {
  let { id, terms, text } = e.data;
  if (!E) return postMessage({ type: 'result', id, items: [], words: [] });
  let topicWords = [];
  if (TP && text) { const m = TP.match(text); topicWords = m.words; terms = terms.concat(m.weighted); }
  const words = [...new Set(terms.flatMap(t => t.x ? E.expandExact(t.w) : E.expand(t.w)))];
  const ws = new Set(words);
  // Boost hadiths where a search word is inside the Prophet's quoted words (the matn), not only in the chain.
  let res = E.search(terms, 400).map(([d, s, h]) => {
    const t = texts[d];
    if (t) {
      const quoted = (t.match(/"[^"]+"/g) || []).join(' ');
      if (quoted && tokens(quoted).some(k => ws.has(k))) s *= 1.8;
    }
    return [d, s, h];
  });
  res.sort((a, b) => b[1] - a[1]); res = res.slice(0, 150);
  postMessage({ type: 'result', id, words, topicWords, items: res.map(([d, s, h]) => [...ref[d], s, h]) });
};
load().catch(err => postMessage({ type: 'error', message: String(err) }));
