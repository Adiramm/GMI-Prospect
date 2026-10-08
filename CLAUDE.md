# GMI Prospect App — Project Spec

Android app for SPG 0562 (Android Programming) PBL Assignment 2, German-Malaysian Institute (GMI).
It promotes GMI courses to SPM leavers: browse courses, check eligibility from SPM results, take a short
interest quiz, see ranked eligible courses, and apply through an admission form.

**Main learning goal: the Android Activity lifecycle.** Every design choice below should make lifecycle
behaviour visible and correct. Code is marked on "Java Class Activities" and "XML Layout", so keep it
readable, commented, and student-level (no over-engineering).

## Tech rules (do not change without asking)
- Language: **Java** only (no Kotlin source files). Layouts: **XML Views** (no Jetpack Compose).
- minSdk 24, target/compile = latest stable the project was created with.
- One screen = one **Activity**. Navigate with explicit `Intent`s + extras. No Fragments, no Navigation component.
- UI: Material Components 3 (`Theme.Material3.DayNight.NoActionBar`), RecyclerView, MaterialCardView, chips.
- No backend, no Firebase, no internet needed. Data is hard-coded in Java.
- Allowed deps: androidx appcompat, material, constraintlayout, recyclerview. Nothing else unless asked.
- Confetti is a **custom Java `ConfettiView`** (Canvas particles), not a library.
- Package: `com.gmi.prospect`.
- Every user-facing string lives in `strings.xml` (English) **and** `values-ms/strings.xml` (Bahasa Melayu).
- After each change, run `./gradlew assembleDebug` (or `gradlew.bat assembleDebug` on Windows) and fix all errors before finishing.

## Lifecycle requirements (core of the assignment)
1. `BaseActivity extends AppCompatActivity` — every screen extends it. It overrides `onCreate`, `onStart`,
   `onResume`, `onPause`, `onStop`, `onRestart`, `onDestroy`, `onSaveInstanceState` and logs each with
   `Log.d("LIFECYCLE", getClass().getSimpleName() + " → onXxx")`. (Logcat filter: `LIFECYCLE`.)
2. **State survives recreation** (rotation, dark-mode switch, language switch):
   - GradeInputActivity: selected grades + added subjects saved in `onSaveInstanceState`, restored in `onCreate`.
   - QuizActivity: current question index + chosen answers saved/restored.
3. **AdmissionFormActivity draft**: save typed fields to SharedPreferences in `onPause`, restore in `onResume`
   (or `onCreate`), clear after successful submit. Show a small "Draft restored" Snackbar when restored.
4. **SplashActivity timer**: use a `Handler` + `Runnable` (~2s). Remove callbacks in `onStop`; restart in `onStart`
   so the app never jumps screens while in background. Splash calls `finish()` after launching Home.
5. **ConfettiView** on ResultActivity: start animation in `onResume`, stop in `onPause` (no wasted CPU in background).
6. Add short comments at each lifecycle override explaining *why* that work happens there (for the report).

## Branding & UI
Banking-app feel: calm and clean, with pops of GMI colour. Rounded everything, soft shadows, generous spacing.

### Colours (from GMI logo: blue #1007A0, red #EF3240)
```
Blue   50 #ECEBF7 100 #D4D2EE 200 #ABA8DE 300 #7C77CB 400 #453EB5 500 #1007A0 600 #0D0683 700 #0A0568 800 #08034D 900 #050233
Red    50 #FEEFF0 100 #FCDADD 200 #F9B7BC 300 #F68E96 400 #F35F6A 500 #EF3240 600 #C42934 700 #9B202A 800 #73181F 900 #4C1014
Neutral 50 #F7F8FC 100 #EEF0F7 200 #DDE1EC 300 #C2C7D6 400 #9AA0B4 500 #6E7489 600 #4E5368 700 #353A4D 800 #1F2233 900 #12141F
```
- Put all of these in `values/colors.xml` as `blue_50`…`red_900`, `neutral_50`…
- Light theme: background neutral_50, surface white, primary blue_500, onPrimary white, accent/pop red_500, text neutral_900 / neutral_600.
- Dark theme (`values-night/themes.xml`): background blue_900, surface blue_800, primary blue_300, accent red_400, text neutral_50 / neutral_300.
- Red is a **pop colour only** (badges, dot, confetti, small highlights) — never large areas.
- Header gradient (Welcome, Home): blue_500 → blue_700 (dark mode: blue_700 → blue_900), with a small red dot motif echoing the logo's "i".

### Typography
- Headings: **Plus Jakarta Sans** (Bold / ExtraBold). Body & buttons: **Manrope** (Regular / Medium / SemiBold).
- Font files go in `res/font/` (lowercase, underscores): `plus_jakarta_sans_bold.ttf`, `plus_jakarta_sans_extrabold.ttf`,
  `manrope_regular.ttf`, `manrope_medium.ttf`, `manrope_semibold.ttf`. Define TextAppearance styles in `themes.xml`/`styles.xml`
  and use them everywhere — never set fonts per-view.

### Shapes & components
- Cards: MaterialCardView, corner radius 20dp, elevation ~2dp (or stroke in dark mode), 16dp padding.
- Buttons: pill-shaped (corner 28dp), height 56dp.
- Grade chips: `ChipGroup` with single selection, rounded.
- Icons: Material vector drawables (add via Vector Asset); keep one consistent outline style.
- Logo: `res/drawable/gmi_logo.xml` (converted from the provided SVG).

## Screens (Activities)
1. **SplashActivity** (launcher) — gradient background, GMI logo, greeting "Hey there 👋" + "Let's find your path at GMI",
   button **"Let's go →"** (also auto-continues after ~2s timer, see lifecycle rule 4). → HomeActivity.
2. **HomeActivity** — gradient header with greeting + logo; top-right icons: theme toggle (light/dark) and language toggle (EN/BM).
   2×2 grid of rounded cards with icons: **Courses**, **Check Eligibility**, **About Us**, **Contact Us**.
   - Theme: `AppCompatDelegate.setDefaultNightMode(...)`, persisted in SharedPreferences, applied in an `Application` subclass.
   - Language: `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ms"|"en"))`
     (+ `AppLocalesMetadataHolderService` `autoStoreLocales=true` in manifest).
3. **CourseListActivity** — `TabLayout` with tabs **Diploma / GAPP / GUFP** filtering one RecyclerView (no ViewPager/Fragments).
   Diploma tab grouped by faculty (Electrical / Mechanical / Computer & Information) with small section headers.
   Each card: icon, course name, duration, optional red "Limited seats" chip. Tap → CourseDetailActivity.
4. **CourseDetailActivity** — name, level, faculty, duration, description, entry requirements (bullet list),
   button **"Am I eligible?"** → GradeInputActivity.
5. **GradeInputActivity** — fixed core subjects: **Bahasa Melayu, English, Mathematics, Sejarah**.
   Each subject row = subject name + horizontally scrollable single-select ChipGroup of grades:
   `A+ A A- B+ B C+ C D E G`. Button **"+ Add subject"** opens a bottom sheet / dialog listing optional subjects
   (see SubjectRepository); added rows have a remove (×) icon. Validation: every row must have a grade, no duplicate
   subjects → otherwise show error (Snackbar + highlight row). **"Next"** → QuizActivity with grades in the Intent.
6. **QuizActivity** — one question per screen, `LinearProgressIndicator` at top ("Question 2 of 6"),
   4 large rounded answer cards; tapping one highlights it, **Next/Back** buttons. Last question → ResultActivity.
7. **ResultActivity** — runs EligibilityChecker + ranking.
   - Eligible: confetti (custom ConfettiView overlay), headline "You're eligible for N courses 🎉",
     ranked list; top item gets a red **"⭐ Best match"** badge. Each item has **Apply** → AdmissionFormActivity.
   - Not eligible: no confetti, friendly message + list of **missing requirements** per level
     (e.g. "Diploma: needs a credit (C) in Mathematics"), buttons **Retake** (→ GradeInputActivity) and **Contact Us**.
   - Button **Home** always available.
8. **AdmissionFormActivity** — fields: full name, IC number (12 digits, format 000000-00-0000), phone, email, address.
   Read-only summary card: chosen course + grades (pre-filled from Intent). Validate all fields (TextInputLayout errors).
   **Submit** → `Intent.ACTION_SENDTO` with `mailto:ghaziadiram@gmail.com`, subject `GMI Application – <Course> – <Name>`,
   body = all details + grade list. Then clear draft (lifecycle rule 3).
9. **AboutActivity** — header image/gradient, short GMI intro (see About text), key facts as small cards.
10. **ContactActivity** — cards: **Call** `+603-8921 9000` (`ACTION_DIAL`), **Email** `marketing@gmi.edu.my` (`ACTION_SENDTO`),
    **Website** `https://www.gmi.edu.my` (`ACTION_VIEW`), **Location** "German-Malaysian Institute, Jalan Ilmiah, Taman Universiti,
    43000 Kajang, Selangor" (`geo:0,0?q=German-Malaysian+Institute+Kajang`).

All inner screens have a top app bar with back arrow (`MaterialToolbar`).

## Data model
```java
enum Level { DIPLOMA, GAPP, GUFP }
enum Faculty { ELECTRICAL, MECHANICAL, COMPUTER, PRE_UNIVERSITY }
enum Interest { ELECTRICAL, MECHANICAL, DESIGN, IT }
enum Grade { A_PLUS("A+"), A("A"), A_MINUS("A-"), B_PLUS("B+"), B("B"), C_PLUS("C+"), C("C"), D("D"), E("E"), G("G");
  // isCredit() = C or better; isPass() = E or better; isA() = A- or better }
class Subject { String id; @StringRes int nameRes; boolean isScienceTechnical; }
class Course  { String id; @StringRes int nameRes, descRes; Level level; Faculty faculty; String duration;
                RuleType rule; List<Interest> interests; boolean limitedSeats; @DrawableRes int icon; }
```
`CourseRepository`, `SubjectRepository`, `QuizRepository` = plain Java classes returning static lists.
Pass data between Activities as `Serializable`/`Parcelable` or `HashMap<String,String>` (subjectId → grade label).

## Courses (from gmi.edu.my, Oct 2026 — only programmes currently offered)
**Diploma — Electrical Engineering** (3 years)
- Diploma of Mechatronics Engineering Technology — interests ELECTRICAL, MECHANICAL
- Diploma of Engineering Technology (Instrumentation & Control) — ELECTRICAL
- Diploma of Electronics Engineering Technology (Computer) — ELECTRICAL, IT
- Diploma in Autotronics Engineering Technology — ELECTRICAL, MECHANICAL
- Diploma in Engineering Technology (Sustainable Energy & Power Distribution) — ELECTRICAL
- Industrial Communication System — ELECTRICAL, IT — *limited seats*
- Facilities & Building Automation — ELECTRICAL — *limited seats*

**Diploma — Mechanical Engineering** (3 years)
- Diploma in Engineering Technology (Industrial Design) — DESIGN, MECHANICAL
- Diploma in Industrial Quality Engineering Technology — MECHANICAL
- Diploma in Innovative Product Design Engineering Technology — DESIGN, MECHANICAL
- Diploma of Mechanical Engineering Technology (CNC Precision) — MECHANICAL
- Diploma of Mechanical Engineering Technology (Manufacturing) — MECHANICAL — *limited seats*
- Tool & Die Technology — MECHANICAL — *limited seats*

**Diploma — Computer & Information** (3 years)
- Diploma in Software Engineering — IT. "Develops the competencies needed across the software development process, including software testing and quality."
- Diploma in Creative Multimedia — DESIGN, IT. "Covers the full 2D/3D digital art pipeline — drawing, modelling, texturing, animation and rendering — using industry-standard tools and a recording studio."

**GAPP — German A-Level Preparatory Programme** — 20 months at GMI + 6 months intensive German (B2 / TestDAF) in Germany,
then entry to a German University of Applied Sciences. Interests ELECTRICAL, MECHANICAL.

**GUFP — GMI–UTP Foundation Programme** — 1 year (3 trimesters), pathway to Universiti Teknologi PETRONAS degrees.
Interests ELECTRICAL, MECHANICAL, IT.

For courses without a description above, write a neutral 1–2 sentence description based on the programme name and
mark it `<!-- TODO verify -->` in strings.xml.

## Eligibility rules (`EligibilityChecker`)
SPM grades: A+ A A- B+ B C+ C D E G. **Credit = C or better. Pass = E or better.**
- **General (all levels):** pass in Bahasa Melayu and Sejarah (needed for an SPM certificate).
- **RuleType.DIPLOMA_GENERAL** (all diplomas): credit in **Mathematics** + credit in **≥1 Science/Technical/Vocational subject**
  + credit in **≥1 other subject** (not the two already counted) + **pass in English**.
- **RuleType.GAPP** (private candidate route): credit (C or better) in **English, Mathematics, Additional Mathematics, Physics, Chemistry**.
- **RuleType.GUFP**: minimum C in **Bahasa Melayu, Mathematics, English, Additional Mathematics, Physics, Chemistry**.
- Also, all diplomas: "no colour blindness / medical check" — show as a note on CourseDetail only, not checked by the app.
- `check(grades)` returns per-course `EligibilityResult { boolean eligible; List<String> missing; }` with human-readable missing items.

Science/Technical subjects (`isScienceTechnical = true`): Physics, Chemistry, Biology, Science, Additional Mathematics,
Computer Science, Engineering Drawing, Mechanical Engineering Studies, Electrical & Electronic Engineering Studies,
Civil Engineering Studies, Invention, Graphic Communication Technology, Agriculture.
Other optional subjects: Pendidikan Islam, Pendidikan Moral, Accounting, Economics, Business, Geography,
Visual Arts, English Literature, Chinese Language, Tamil Language, Arabic.

## Interest quiz (`QuizRepository`) — answer index maps to Interest: 0 ELECTRICAL, 1 MECHANICAL, 2 DESIGN, 3 IT
1. What sounds most fun? — Building a circuit that lights up / Taking apart an engine / Sketching a cool product / Coding a small game
2. Your ideal weekend project? — Wiring smart lights at home / Fixing your motorbike / Designing a poster or 3D model / Building an app or website
3. Which subject do you enjoy most? — Physics / Add Maths / Visual Arts / Computer Science
4. In a group project you usually… — Make the gadget work / Build the physical model / Make it look good / Handle the tech & data
5. Which video would you click? — "How solar panels work" / "Inside a Formula 1 engine" / "Designing the iPhone" / "How hackers break in"
6. Dream workplace? — Power plant or factory automation / Car or aerospace plant / Design studio / Tech company
Scoring: +1 to the chosen Interest per question.
**Ranking:** among eligible courses, score = average of quiz scores of the course's interests; sort desc; ties → Diploma before GAPP/GUFP, then name.
Quiz never changes eligibility — it only orders the list.

## About Us text (draft — team to verify)
"The German-Malaysian Institute (GMI) in Kajang, Selangor, was set up in 1992 through cooperation between the
governments of Malaysia and Germany. GMI trains future engineers and technologists using a German-style,
hands-on approach — small classes, industry-standard workshops and strong links with industry. Programmes range
from engineering technology diplomas to pre-university pathways to universities in Germany (GAPP) and UTP (GUFP)."

## Project structure
```
com.gmi.prospect
├─ GmiApp.java                (Application: apply saved theme)
├─ base/BaseActivity.java     (lifecycle logging)
├─ ui/  SplashActivity, HomeActivity, CourseListActivity, CourseDetailActivity, GradeInputActivity,
│       QuizActivity, ResultActivity, AdmissionFormActivity, AboutActivity, ContactActivity
├─ ui/adapter/  CourseAdapter, ResultAdapter
├─ ui/view/ConfettiView.java
├─ data/  Course, Subject, Grade, Level, Faculty, Interest, RuleType, QuizQuestion,
│         CourseRepository, SubjectRepository, QuizRepository
└─ logic/ EligibilityChecker, EligibilityResult, CourseRanker
```
Layouts named `activity_<screen>.xml`, `item_<thing>.xml`.

## Working style
- Build in the phases the user gives you; don't jump ahead.
- Keep methods short, add Javadoc on each class and comments on lifecycle methods.
- After finishing a phase: build, then list the files you created/changed and how to test the phase on the emulator.
