# DyslexiRead — Version Kotlin / Android natif 🤖

Application Android native en **Kotlin** pour lire tout document en police **OpenDyslexic**, avec OCR, export PDF et lecture audio hors-ligne (FR, EN, IT).

---

## Architecture

```
app/src/main/
├── java/com/dyslexiread/
│   ├── DyslexiReadApp.kt              # Application class
│   ├── models/
│   │   └── Models.kt                  # ReaderSettings, LoadedDocument, TtsLanguage
│   ├── services/
│   │   ├── OcrService.kt              # ML Kit Text Recognition (hors-ligne)
│   │   ├── DocumentService.kt         # Extraction PDF (iText7) + DOCX (POI)
│   │   ├── TtsService.kt              # TTS natif Android, chunking auto
│   │   └── PdfExportService.kt        # Génération PDF OpenDyslexic + partage
│   └── ui/
│       ├── MainActivity.kt            # Single Activity host
│       ├── ReaderViewModel.kt         # ViewModel partagé (MVVM)
│       ├── home/
│       │   └── HomeFragment.kt        # Écran d'accueil (4 sources)
│       ├── reader/
│       │   └── ReaderFragment.kt      # Lecteur + barre TTS + export
│       └── settings/
│           └── SettingsBottomSheet.kt # Panneau personnalisation
├── res/
│   ├── layout/                        # XML layouts (ViewBinding)
│   ├── navigation/nav_graph.xml       # Navigation Component
│   ├── drawable/                      # Icônes vectorielles
│   ├── font/                          # Famille OpenDyslexic
│   ├── values/                        # Colors, strings, themes
│   ├── menu/reader_menu.xml           # Menu toolbar
│   ├── anim/                          # Transitions de navigation
│   └── xml/file_paths.xml             # FileProvider paths
└── AndroidManifest.xml
```

**Pattern MVVM** : `HomeFragment` / `ReaderFragment` → `ReaderViewModel` → Services

---

## Fonctionnalités

| Fonctionnalité | Implémentation |
|---|---|
| 📷 OCR photo | ML Kit `TextRecognition` (hors-ligne, script Latin) |
| 📄 Lecture PDF | iText7 `PdfTextExtractor` |
| 📝 Lecture DOCX | Apache POI `XWPFDocument` |
| 🔤 Police OpenDyslexic | `.ttf` dans `assets/fonts/` + `res/font/` |
| 🎨 7 fonds de couleur | Palettes optimisées pour la dyslexie |
| 🔊 TTS hors-ligne | `android.speech.tts.TextToSpeech` natif |
| 🇫🇷🇺🇸🇮🇹 FR / EN / IT | `Locale.FRENCH`, `Locale.US`, `Locale.ITALIAN` |
| 📤 Export PDF | iText7 + `FileProvider` + `Intent.ACTION_SEND` |
| 🌙 Mode sombre | `DayNight` Material3 |
| 📂 Ouverture externe | Intent `ACTION_VIEW` PDF/DOCX depuis d'autres apps |

---

## Installation

### Prérequis
- Android Studio Hedgehog (2023.1) ou plus récent
- SDK Android 24+ (Android 7.0 minimum)
- JDK 17

### 1. Cloner

```bash
git clone <repo>
cd dyslexiread-kotlin
```

### 2. Ajouter la police OpenDyslexic

Télécharger sur [opendyslexic.org](https://opendyslexic.org) (licence SIL OFL 1.1, gratuite) et placer les fichiers à **deux endroits** :

```
# Pour l'affichage dans les TextView (XML fonts)
app/src/main/res/font/
├── open_dyslexic.ttf
├── open_dyslexic_bold.ttf
└── open_dyslexic_italic.ttf

# Pour l'export PDF (iText7 via assets)
app/src/main/assets/fonts/
├── OpenDyslexic-Regular.ttf
├── OpenDyslexic-Bold.ttf
└── OpenDyslexic-Italic.ttf
```

### 3. Synchroniser et lancer

```bash
# Depuis Android Studio : File > Sync Project with Gradle Files
# Puis Run > Run 'app'

# Ou en ligne de commande :
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## TTS hors-ligne — Configuration

Le TTS utilise le moteur **natif Android** (Google TTS ou autre selon l'appareil).

Pour activer les voix françaises et italiennes hors-ligne :
1. **Paramètres** → **Accessibilité** → **Synthèse vocale**
2. Sélectionner **Google** comme moteur préféré
3. Appuyer sur ⚙ → **Langues installées** → Télécharger FR et IT

> Sur certains appareils Samsung ou Xiaomi, le moteur par défaut peut différer. L'app affiche `⚠ Voix indisponible` si la langue n'est pas installée.

---

## Dépendances clés

| Bibliothèque | Version | Usage |
|---|---|---|
| `google_mlkit_text_recognition` | 16.0.0 | OCR hors-ligne |
| `itext7-core` | 7.2.6 | Lecture + export PDF |
| `poi-ooxml` | 5.2.5 | Lecture DOCX |
| `androidx.navigation` | 2.7.7 | Navigation Fragment |
| `androidx.lifecycle` | 2.8.3 | ViewModel + LiveData |
| `camerax` | 1.3.4 | Capture photo |
| `material` | 1.12.0 | Material3 UI |

---

## Licence iText7

iText7 est sous licence **AGPL v3** (libre pour projets open source).
Pour un usage commercial, une licence commerciale iText est nécessaire.
Alternative libre : **Apache PDFBox** (moins performant sur Android).

---

## Comparaison avec la version Flutter

| Critère | Kotlin (natif) | Flutter (cross-platform) |
|---|---|---|
| Performance | ⭐⭐⭐⭐⭐ Natif | ⭐⭐⭐⭐ Très bon |
| iOS support | ❌ Android only | ✅ iOS + Android |
| Taille APK | ~25 Mo | ~20-30 Mo |
| Intégration système | ⭐⭐⭐⭐⭐ Maximale | ⭐⭐⭐⭐ Très bonne |
| Maintenance | Android only | Un seul code base |

---

## Évolutions possibles

- [ ] Support Google Docs (API Drive + OAuth2)
- [ ] Surlignage mot par mot pendant la lecture (TTS Boundary Events)
- [ ] OCR en temps réel via CameraX (viewfinder live)
- [ ] Widget Android pour lancer la caméra rapidement
- [ ] Export EPUB
- [ ] Profils utilisateur (SharedPreferences / DataStore)
- [ ] Tile accès rapide dans le panneau de notifications
