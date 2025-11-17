# Tesseract Trained Data Files

This directory should contain Tesseract trained data files (`.traineddata`) for the languages you want to support.

## Download Language Files

Trained data files can be downloaded from the official Tesseract repository:

**Best (Recommended):**
- URL: https://github.com/tesseract-ocr/tessdata_best
- High accuracy, larger file sizes (~10-50 MB per language)
- Best for production use

**Fast:**
- URL: https://github.com/tesseract-ocr/tessdata_fast
- Faster processing, smaller file sizes (~1-5 MB per language)
- Good for quick prototyping

**Standard:**
- URL: https://github.com/tesseract-ocr/tessdata
- Balanced accuracy and speed

## How to Add Languages

1. Download the desired `.traineddata` file from one of the above repositories
2. Place the file in this directory (`app/src/main/assets/tessdata/`)
3. The file will be automatically copied to the app's external storage on first use

## Example: Adding English Support

```bash
cd app/src/main/assets/tessdata/
wget https://github.com/tesseract-ocr/tessdata_best/raw/main/eng.traineddata
```

## Commonly Used Languages

- `eng.traineddata` - English
- `fra.traineddata` - French
- `deu.traineddata` - German
- `spa.traineddata` - Spanish
- `ita.traineddata` - Italian
- `por.traineddata` - Portuguese
- `rus.traineddata` - Russian
- `ara.traineddata` - Arabic
- `chi_sim.traineddata` - Chinese Simplified
- `chi_tra.traineddata` - Chinese Traditional
- `jpn.traineddata` - Japanese
- `kor.traineddata` - Korean
- `hin.traineddata` - Hindi

See full list: https://tesseract-ocr.github.io/tessdoc/Data-Files-in-different-versions.html

## File Size Considerations

- Each language file can be 10-50 MB
- Include only languages you need to keep APK size small
- Consider downloading languages on-demand from the app (future feature)

## Note

**IMPORTANT:** Language files are NOT included in the repository due to their size.
Developers must download and add them manually or implement runtime download functionality.

The `.gitignore` file is configured to ignore `*.traineddata` files to prevent accidental commits.
