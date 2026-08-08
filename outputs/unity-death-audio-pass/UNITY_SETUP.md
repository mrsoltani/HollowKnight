# Unity setup: player death and layered audio

## Important repository note

The workspace inspected here is a Java/LibGDX project, not a Unity/C# project, so these are modular Unity scripts rather than direct patches to the current source tree. The audit did confirm the same issues described in the request: a 1.5-second death delay, duplicate damage/death SFX registrations, `Hero Death Extra Details.mp3` present but unused, and available `ShadowScream` atlas frames.

## 1. AudioManager

1. Create an `AudioManager` GameObject in the bootstrap scene and add `AudioManager.cs`.
2. Assign the SFX mixer group if the project uses an `AudioMixer`.
3. In **Player Damage Clips**, assign the available player hurt variants.
4. Assign:
   - **Player Death Main**: `Hero Death V2.mp3`
   - **Player Death Details**: `Hero Death Extra Details.mp3`
5. Keep only one `AudioManager` in play. The component persists with `DontDestroyOnLoad` and rejects duplicates.
6. Remove player damage/death playback from enemy hitboxes, animation events, GameManager, and respawn code. Those systems should only call `PlayerHealth.TakeDamage(...)`. `PlayerHealth` is the sole owner of `PlayPlayerDamage()` and `PlayPlayerDeath()` calls.

The two death clips are played on separate pooled `AudioSource` voices in the same frame, so they layer instead of interrupting one another.

## 2. PlayerHealth

1. Replace or merge the health/death responsibility of the existing health component with `PlayerHealth.cs`.
2. Assign:
   - Player `Animator`
   - Player `Rigidbody2D`
   - All player colliders
   - Movement/input/attack behaviours to **Disable While Dead**
   - `ShadowScream` prefab and effect origin
   - `CameraShake2D`
   - `ScreenFader`
   - A component implementing `IPlayerRespawnService`, or a fallback respawn Transform
3. Animator parameters expected by default:
   - `Hurt` trigger
   - `Death` trigger
   - `Respawn` trigger
   Rename the serialized fields if the controller already uses different names.
4. All timings are unscaled seconds. The default sequence is roughly:
   - 0.12 s freeze frame
   - ShadowScream + layered death SFX + 0.65 s camera shake
   - 0.85 s burst hold
   - 0.35 s pre-fade pause
   - 0.65 s fade to black
   - 0.45 s black hold and respawn
   - 0.75 s fade back in

## 3. ShadowScream prefab

1. Import/slice the `ShadowScream` frames as sprites.
2. Create a non-looping `ShadowScream` AnimationClip.
3. Make it the default state in a small Animator Controller with no transition required.
4. Create a prefab containing:
   - `SpriteRenderer`
   - `Animator`
   - `DestroyAfterAnimation`
5. Set sorting layer/order so the burst renders above the player and most foreground gameplay sprites.

No `ShadowScream` Animator trigger is necessary with the prefab approach: spawning the prefab starts its default animation immediately. This keeps the effect independent of the player's controller and sprite renderer.

## 4. Camera shake

Recommended hierarchy:

```text
CameraRig (follow/position logic)
└── ShakePivot (CameraShake2D)
    └── Main Camera
```

Assign `ShakePivot`'s `CameraShake2D` to `PlayerHealth`. If using Cinemachine, prefer a Cinemachine Impulse implementation and call it from the same death hook; do not put this positional shake on a transform Cinemachine overwrites every frame.

## 5. Screen fader

1. Create a Screen Space Overlay Canvas with a high sorting order.
2. Add a full-screen black `Image`, a `CanvasGroup`, and `ScreenFader` to the same object.
3. Disable **Raycast Target** on the Image; `ScreenFader` controls blocking through the CanvasGroup.
4. Assign the component to `PlayerHealth`.

## Integration contract

Normal damage should reduce to one call:

```csharp
playerHealth.TakeDamage(damageAmount);
```

Fatal hazards can use:

```csharp
playerHealth.KillImmediately();
```

Do not follow either call with audio playback. This prevents duplicate damage/death cues and guarantees the death sequence runs only once.

## Unity version compatibility

`PlayerHealth.cs` uses `Rigidbody2D.linearVelocity`, the current Unity API. On older Unity versions that expose only `velocity`, replace the two `linearVelocity` assignments with `velocity`.
