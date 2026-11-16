# Symbols of list_mode: where each comes from

One white symbol per package of `ListMode.symbolFor` (15 rows). Every vector in `res/drawable/ic_list_*.xml` carries
`Package:`, source and license in its header comment. Date of this table: 2026-11-02.

Simple Icons (https://github.com/simple-icons/simple-icons), version 16.36.0, files `icons/<slug>.svg`, licence CC0 1.0
(`LICENSE.md` of that version). The paths were taken over unchanged (arc flags written with spaces for Android); wide
word marks were cropped to their outline and drawn 48 dp wide, the others sit in a 36 dp square with `viewport 24`.

Trademarks: the drawings are free, the brands belong to their owners. They are used here only to mark the matching app on one
TV's own home screen.

| Package | Name in the list | Source | Licence | Date | File |
|---|---|---|---|---|---|
| `com.netflix.ninja` | app's own name | Simple Icons slug `netflix` (a version older than 16.36.0) | CC0 | 2026-10-31 | `ic_list_netflix.xml` |
| `com.google.android.youtube.tv` | app's own name | Simple Icons slug `youtube` (a version older than 16.36.0) | CC0 | 2026-10-31 | `ic_list_youtube.xml` |
| `com.amazon.amazonvideo.livingroom` | app's own name | Simple Icons slug `primevideo` (a version older than 16.36.0; the slug is gone in 16.36.0) | CC0 | 2026-10-31 | `ic_list_primevideo.xml` |
| `com.crunchyroll.crunchyroid` | app's own name | Simple Icons 16.36.0, slug `crunchyroll` | CC0 | 2026-11-02 | `ic_list_crunchyroll.xml` |
| `com.spotify.tv.android` | app's own name | Simple Icons 16.36.0, slug `spotify` | CC0 | 2026-11-02 | `ic_list_spotify.xml` |
| `com.zdf.android.mediathek` | app's own name | Simple Icons 16.36.0, slug `zdf` | CC0 | 2026-11-02 | `ic_list_zdf.xml` |
| `com.valvesoftware.steamlink` | app's own name | Simple Icons 16.36.0, slug `steam` (the Steam mark, not Steam Link) | CC0 | 2026-11-02 | `ic_list_steam.xml` |
| `com.ted.android.tv` | app's own name | Simple Icons 16.36.0, slug `ted` | CC0 | 2026-11-02 | `ic_list_ted.xml` |
| `com.android.vending` | app's own name | Simple Icons 16.36.0, slug `googleplay` | CC0 | 2026-11-02 | `ic_list_googleplay.xml` |
| `de.telekom.magentatv.atv` | app's own name | Simple Icons 16.36.0, slug `deutschetelekom` (the T; Magenta TV has no slug) | CC0 | 2026-11-02 | `ic_list_magenta.xml` |
| `de.swr.avp.ard.tv` | ARD Mediathek (`app_labels` of the `sony-bravia-de` theme) | drawn by hand: word mark "ARD", strokes with round ends | free (own drawing) | 2026-11-02 | `ic_list_ard.xml` |
| `tv.arte.plus7` | app's own name | drawn by hand: word mark "ARTE" | free (own drawing) | 2026-11-02 | `ic_list_arte.xml` |
| `com.disney.disneyplus` | app's own name | drawn by hand: word mark "DISNEY+" (no slug in 16.36.0) | free (own drawing) | 2026-11-02 | `ic_list_disneyplus.xml` |
| `de.sky.online` | app's own name | drawn by hand: word mark "WOW" (slug `sky` is another mark) | free (own drawing) | 2026-11-02 | `ic_list_wow.xml` |
| `de.prosiebensat1digital.seventv` | app's own name | drawn by hand: word mark "7TV" (slug `prosieben` is another mark) | free (own drawing) | 2026-11-02 | `ic_list_seventv.xml` |

Every other app, Tagesschau included, gets a monogram (`MonogramDrawable`: white ring, first letter of its name), drawn in code, no file.
