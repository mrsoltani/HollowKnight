# Hollow Knight Clone

*A hand-built love letter to Hollow Knight — 14,700 lines of Java, 115 source files, 13 hand-drawn
maps, and not a single line of borrowed game logic.*

One student, one quarter, and an unreasonable amount of attention to detail. Everything here — the
weight of a jump, the hitch before a boss swing, the way a beam of light charges before it kills you
— was tuned by hand.

---

## ✨ The Shining Things

**A knight that actually feels like one.**
Twenty animation states, stitched together by hand. Release jump early and gravity bites 2.5× harder.
Cling to a wall and slide; jump off it and you get 150 ms of grace to make the leap anyway. Dash and
the world blurs. Swing your nail mid-air and time *stalls* for a tenth of a second. Miss a platform
and you fall, and land, and the sound of the landing changes depending on how far you fell.

**Pogo.** Down-slash onto an enemy — or a spike — and bounce. It refills your dash, re-arms your
double jump, and turns the whole world into a trampoline. Nobody asked for it. It's in there.

**A boss with a memory.**
The False Knight has 300 HP and reads you. It tracks your velocity and leaps to where you're *going*,
not where you are. It refuses to repeat the same move twice in a row. At half health the ceiling
breaks, its armour falls away, and it comes back **fast** — 30% quicker, harder-hitting, with a power
slam that sends shockwaves rolling across the floor in both directions. A door slams shut behind you
the moment you step in.

**Four enemies, four personalities.**
The patroller that turns at ledges. The horned husk that freezes, winds up, and *lunges*. The
mosquito that hovers at the edge of your reach before diving. And the Crystal Guardian, which sees
you, charges an orb, and fires a beam clean off the edge of the screen — then enrages, charges, and
stalks back home when it's done.

**Light that kills.** Ceiling lasers breathe. Each one cycles through its own randomized rhythm —
charge, fire, rest — deliberately staggered so a corridor of them never syncs into a pattern you can
time. Stalactites hang overhead and drop the instant you cross the wrong inch of floor.

**Walls that remember they're broken.** Three hits with your nail and a hidden wall shatters. It
shakes on each strike. And it *stays* broken, forever, in your save file.

**Eight charms, three notches.**
Soul Catcher. Dashmaster. Unbreakable Strength. Quick Slash. Quick Focus. Heavy Blow. And two you
have to *earn*: **Sharp Shadow**, locked behind a rotating-platform puzzle where the safe side of a
platform depends on which way you hit it — solve it and a full-screen quote blooms across the dark.
And **Void Heart**, sitting in a glowing orb somewhere in Crossroads, which rewrites your spells into
their shadow forms and makes them hit half again as hard.

**Soul.** One key. Four meanings. Tap it on the ground and you launch a fireball. Tap it in the air
and you hang there while you cast. Hold it and you kneel and heal. Hold *Up* and tap, and you scream
a column of wraiths into the sky. Thirty-three Soul each — enough to make every cast a decision.

**A world that sounds like a world — and was tuned like one.**
Five soundtracks cross-fade as you cross borders — two seconds of one melting into the next. But the
real obsession is in the small sounds. *No footstep is ever the same twice*: every step is
pitch-jittered inside a band so the rhythm breathes instead of going mechanical. And one bare
footstep foley is re-timbered per creature — the horned husk lands low and heavy, the crystals land
high and sharp — a single recorded clip, four different beasts. Your nail swings are pulled from
random variants so they never sound canned. The False Knight keeps a whole arsenal of clangs and
thuds, picking its armour hits and head hits at random and *changing its voice* between phase one and
the enraged phase two. One continuous footstep loop rides a single audio channel that cleanly cuts
the last step so they never pile into mush; the Crystal Guardian's beam hums on its own channel that
starts and stops on a dime. Even death is two tracks stacked on top of each other.

**Atmosphere everywhere.** Dust drifts in two depth layers across every room. Backgrounds lerp from
the blue of Crossroads to the plum-dark of Crystal Peak. Menus sit under breathing light beams,
drifting fog, and slow particles — and every theme has its own colour, density, and mood.

**Details nobody will notice, and that's the point.**
Your Soul vessel is a glass orb that fills with liquid, shader-masked, with its own idle, growing and
shrinking animations. Health masks fill and empty one at a time. Achievements slide in and queue so
two unlocks never talk over each other. The end screen reveals itself in five staged beats — title,
then your kills, then your deaths, then your total time, then a quote. Menus opened mid-game blur the
frozen frame behind them. Locked achievement icons render in grayscale.

**Built to be finished, not demoed.** Four save slots backed by a real SQLite database that survives
version changes. Full key rebinding with drawn keycaps. English and French, switchable live. Five
achievements. A guide carousel teaching every ability. A cheat sheet, because of course.

---

## Running It

```bash
git clone https://github.com/mrsoltani/HollowKnight.git
cd HollowKnight

./gradlew lwjgl3:run        # Linux / macOS
gradlew.bat lwjgl3:run      # Windows
```

Requires **Java 21**. Launches fullscreen.

Build a standalone jar with `./gradlew lwjgl3:jar` (or `jarWin` / `jarLinux` / `jarMac`).

---

## Built With

Java 21 · LibGDX 1.14.1 · LWJGL3 · Gradle · Tiled · SQLite · custom GLSL shaders

---

## Disclaimer

A non-commercial university project. **Hollow Knight** and all related intellectual property belong
to Team Cherry. This project is not affiliated with, endorsed by, or associated with Team Cherry.
Art, music, and sound are used for educational purposes only.

## License

MIT — see [`LICENSE`](LICENSE).
