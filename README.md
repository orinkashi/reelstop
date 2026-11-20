# ReelStop 🚫

App Android kat7bes Instagram Reels, YouTube Shorts w TikTok.
Instagram w YouTube kaykhdmo 3adi (messages, stories, videos 3adiyin). Ghir reels li kaytsddo.

---

## Tari9a 1: GitHub kaybni lik l APK (bla Android Studio) ✅ sahla

1. Dir compte f **github.com** (fabor).
2. Clique **New repository** → smmih `reelstop` → **Create**.
3. Clique **uploading an existing file** → jerr **ga3 les fichiers w les dossiers** li f had dossier
   (b7al `app`, `.github`, `build.gradle`...) → **Commit changes**.
   ⚠️ Dossier `.github` mkhbbi f bzaf dyal les PCs. Ila ma banch lik, dir **Add file → Create new file**,
   ktb smiya `.github/workflows/build.yml` w lse9 fih l contenu dyal l fichier.
4. Sir l onglet **Actions** → tsenna chi 3-5 d9aye9 7ta ywli ✅ khder.
5. Clique 3la l build → l ta7t f **Artifacts** → telecharger **ReelStop-apk** (zip, fih `app-debug.apk`).

## Tari9a 2: Android Studio
Ftah dossier `reelstop` f Android Studio → tsenna Gradle ysali → ▶️ Run (telephone mconnecti b USB).

---

## Installation f telephone

1. Sift `app-debug.apk` l telephone w ftho → **Installer** (ila tlab, khlli "sources inconnues").
2. Ftah **ReelStop** → **Activer ReelStop** → l9a ReelStop f la liste → che3lo.
3. ⚠️ **Android 13+**: ila l bouton ma bghach ytche3el ("Paramètre restreint"):
   - Sir **Paramètres → Applications → ReelStop**
   - Clique **⋮** (l fo9 limen) → **Autoriser les paramètres restreints**
   - Rje3 w che3lo 3awd.
4. F chi telephones (Xiaomi, Oppo, Samsung...): f **Batterie**, dir ReelStop "**Sans restriction**",
   bach ma ytfach bo7do.

## Ila chi 7aja ma khdmatch
Instagram w YouTube kaybdlo l code dyal l'écran dyalhom m3a les mises à jour.
Ila reels b9aw kaybano, goul liya ach mn version 3ndek w nzido les IDs jdad
f `BlockerService.java`.
