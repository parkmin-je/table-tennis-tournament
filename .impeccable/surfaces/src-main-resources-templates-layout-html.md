---
version: 1
slug: "src-main-resources-templates-layout-html"
primary_target: "src/main/resources/templates/layout.html"
related_targets: ["src/main/resources/templates/tournament/board.html","src/main/resources/templates/manage/console.html"]
---

## Scope

Whole web app redesign: public tournament pages (Operate/Read), live TV board (Operate, read at 5 to 15 m), staff console (Operate). Home page first viewport leans Persuade but stays a working surface. User delegated every choice ("알아서"); decision page skipped at the user's instruction.

## Direction contract

THESIS: The tournament is the sheet on the gym wall. Every bracket, group table and live board reads like the paper bracket a club tapes up and fills in with markers, where a winner's line is traced in red as they advance. Refuses the default SaaS card dashboard with one blue accent.

OWN-WORLD: Pale painted-block wall ground, white paper sheets with crisp ink rules, marker set as the palette with fixed jobs: ink black for text, red marker for winner paths and live, blue marker for actions and links, green marker for confirmed and done, yellow highlighter for "me" and selection. Heavy black header band. Small radii on sheets, 8px on controls. Pretendard throughout, heavy weights for names, tabular numerals.

STORY: A player opens the page, finds their own name highlighted in yellow on the sheet, sees whether they advanced in red, and which table is calling them. Staff see the next job and the free tables at once.

FIRST VIEWPORT: Header band with the association name and nav. When a tournament is live, the home page opens on its wall: the live table board as a timetable (table number, round, two names) beside the next calls; otherwise the pinned sheets of open tournaments.

FORM: Wall bracket sheet, candidate 5 of 7 (1 flip scoreboard, 2 racket rubber colors, 3 table geometry, 4 gym floor lines, 5 wall bracket sheet, 6 award certificate, 7 ball box). seed:9c10d371a103397f

RAISES: split-flap board (competitive): TV board is a fixed-column timetable, rows keep position and only values change, a changed row flashes once in highlighter. star atlas: one weight ramp, bracket rules thicken toward the final. centre rail: states are marks, withdrawn names struck through, BYE dashed, pending hollow. Crouwel grid: graph-paper ruling only under bracket sheets. character goods: every entry wears the same name plate, name over club. developer console: destructive actions isolated by space and outline-only.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
