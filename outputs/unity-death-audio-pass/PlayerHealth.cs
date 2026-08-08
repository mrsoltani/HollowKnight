using System.Collections;
using UnityEngine;
using UnityEngine.Events;

/// <summary>
/// Owns player health and guarantees that damage/death feedback is fired once.
/// Other systems should call TakeDamage; they should not play player hurt/death audio themselves.
/// </summary>
[DisallowMultipleComponent]
public sealed class PlayerHealth : MonoBehaviour
{
    [System.Serializable]
    public sealed class HealthChangedEvent : UnityEvent<int, int> { }

    [Header("Health")]
    [SerializeField, Min(1)] private int maxHealth = 5;
    [SerializeField] private bool invulnerable;

    [Header("Player References")]
    [SerializeField] private Animator playerAnimator;
    [SerializeField] private Rigidbody2D body;
    [SerializeField] private Collider2D[] playerColliders;
    [Tooltip("Movement, attack, and input behaviours disabled for the entire death sequence.")]
    [SerializeField] private Behaviour[] disableWhileDead;

    [Header("Death Visuals")]
    [Tooltip("Prefab whose Animator automatically plays the ShadowScream clip on spawn.")]
    [SerializeField] private GameObject shadowScreamPrefab;
    [SerializeField] private Transform deathEffectOrigin;
    [SerializeField] private CameraShake2D cameraShake;
    [SerializeField] private ScreenFader screenFader;

    [Header("Respawn")]
    [Tooltip("Optional component implementing IPlayerRespawnService. If empty, fallbackRespawnPoint is used.")]
    [SerializeField] private MonoBehaviour respawnService;
    [SerializeField] private Transform fallbackRespawnPoint;

    [Header("Sequence Timing (unscaled seconds)")]
    [SerializeField, Min(0f)] private float freezeFrameDuration = 0.12f;
    [SerializeField, Min(0f)] private float burstHoldDuration = 0.85f;
    [SerializeField, Min(0f)] private float preFadeDelay = 0.35f;
    [SerializeField, Min(0.01f)] private float fadeOutDuration = 0.65f;
    [SerializeField, Min(0f)] private float blackScreenDuration = 0.45f;
    [SerializeField, Min(0.01f)] private float fadeInDuration = 0.75f;

    [Header("Camera Impact")]
    [SerializeField, Min(0f)] private float deathShakeDuration = 0.65f;
    [SerializeField, Min(0f)] private float deathShakeStrength = 0.55f;

    [Header("Animator Parameters")]
    [SerializeField] private string hurtTrigger = "Hurt";
    [SerializeField] private string deathTrigger = "Death";
    [SerializeField] private string respawnTrigger = "Respawn";

    [Header("Events")]
    [SerializeField] private HealthChangedEvent onHealthChanged = new HealthChangedEvent();
    [SerializeField] private UnityEvent onDamaged = new UnityEvent();
    [SerializeField] private UnityEvent onDeathStarted = new UnityEvent();
    [SerializeField] private UnityEvent onRespawned = new UnityEvent();

    private int currentHealth;
    private bool isDead;
    private Coroutine deathRoutine;
    private IPlayerRespawnService cachedRespawnService;
    private float timeScaleBeforeFreeze = 1f;
    private bool ownsTimeFreeze;

    public int CurrentHealth { get { return currentHealth; } }
    public int MaxHealth { get { return maxHealth; } }
    public bool IsDead { get { return isDead; } }

    private void Awake()
    {
        currentHealth = maxHealth;
        cachedRespawnService = respawnService as IPlayerRespawnService;

        if (respawnService != null && cachedRespawnService == null)
        {
            Debug.LogError(respawnService.name + " must implement IPlayerRespawnService.", respawnService);
        }
    }

    private void Start()
    {
        onHealthChanged.Invoke(currentHealth, maxHealth);
    }

    /// <summary>
    /// The only public entry point for normal player damage.
    /// Returns true only when damage was actually accepted.
    /// </summary>
    public bool TakeDamage(int amount)
    {
        if (amount <= 0 || invulnerable || isDead)
        {
            return false;
        }

        currentHealth = Mathf.Max(0, currentHealth - amount);
        onHealthChanged.Invoke(currentHealth, maxHealth);

        if (currentHealth == 0)
        {
            BeginDeath();
        }
        else
        {
            TriggerAnimator(playerAnimator, hurtTrigger);
            if (AudioManager.Instance != null)
            {
                AudioManager.Instance.PlayPlayerDamage();
            }
            onDamaged.Invoke();
        }

        return true;
    }

    public void KillImmediately()
    {
        if (isDead)
        {
            return;
        }

        currentHealth = 0;
        onHealthChanged.Invoke(currentHealth, maxHealth);
        BeginDeath();
    }

    public void Heal(int amount)
    {
        if (amount <= 0 || isDead)
        {
            return;
        }

        int nextHealth = Mathf.Min(maxHealth, currentHealth + amount);
        if (nextHealth == currentHealth)
        {
            return;
        }

        currentHealth = nextHealth;
        onHealthChanged.Invoke(currentHealth, maxHealth);
    }

    private void BeginDeath()
    {
        if (isDead)
        {
            return;
        }

        isDead = true;
        SetPlayerControlEnabled(false);
        SetCollidersEnabled(false);

        if (body != null)
        {
            body.linearVelocity = Vector2.zero;
            body.angularVelocity = 0f;
        }

        TriggerAnimator(playerAnimator, deathTrigger);
        onDeathStarted.Invoke();

        // Death audio has one owner. Do not also call it from an Animation Event,
        // enemy hitbox, GameManager, or respawn controller.
        if (AudioManager.Instance != null)
        {
            AudioManager.Instance.PlayPlayerDeath();
        }

        if (deathRoutine != null)
        {
            StopCoroutine(deathRoutine);
        }

        deathRoutine = StartCoroutine(DeathSequence());
    }

    private IEnumerator DeathSequence()
    {
        timeScaleBeforeFreeze = Time.timeScale;
        ownsTimeFreeze = true;
        Time.timeScale = 0f;
        yield return new WaitForSecondsRealtime(freezeFrameDuration);
        RestoreTimeScale();

        SpawnShadowScream();

        if (cameraShake != null)
        {
            cameraShake.Shake(deathShakeDuration, deathShakeStrength);
        }

        yield return new WaitForSecondsRealtime(burstHoldDuration);
        yield return new WaitForSecondsRealtime(preFadeDelay);

        if (screenFader != null)
        {
            yield return screenFader.FadeOut(fadeOutDuration);
        }

        yield return new WaitForSecondsRealtime(blackScreenDuration);
        PerformRespawn();

        currentHealth = maxHealth;
        onHealthChanged.Invoke(currentHealth, maxHealth);
        TriggerAnimator(playerAnimator, respawnTrigger);

        if (screenFader != null)
        {
            yield return screenFader.FadeIn(fadeInDuration);
        }

        SetCollidersEnabled(true);
        SetPlayerControlEnabled(true);
        isDead = false;
        deathRoutine = null;
        onRespawned.Invoke();
    }

    private void SpawnShadowScream()
    {
        if (shadowScreamPrefab == null)
        {
            Debug.LogWarning("No ShadowScream prefab is assigned.", this);
            return;
        }

        Transform origin = deathEffectOrigin != null ? deathEffectOrigin : transform;
        Instantiate(shadowScreamPrefab, origin.position, origin.rotation);
    }

    private void PerformRespawn()
    {
        if (cachedRespawnService != null)
        {
            cachedRespawnService.RespawnPlayer(this);
        }
        else if (fallbackRespawnPoint != null)
        {
            transform.position = fallbackRespawnPoint.position;
        }
        else
        {
            Debug.LogError("No respawn service or fallback respawn point is assigned.", this);
        }

        if (body != null)
        {
            body.position = transform.position;
            body.linearVelocity = Vector2.zero;
            body.angularVelocity = 0f;
        }
    }

    private void SetPlayerControlEnabled(bool enabled)
    {
        for (int i = 0; i < disableWhileDead.Length; i++)
        {
            if (disableWhileDead[i] != null)
            {
                disableWhileDead[i].enabled = enabled;
            }
        }
    }

    private void SetCollidersEnabled(bool enabled)
    {
        for (int i = 0; i < playerColliders.Length; i++)
        {
            if (playerColliders[i] != null)
            {
                playerColliders[i].enabled = enabled;
            }
        }
    }

    private static void TriggerAnimator(Animator animator, string triggerName)
    {
        if (animator != null && !string.IsNullOrWhiteSpace(triggerName))
        {
            animator.SetTrigger(triggerName);
        }
    }

    private void RestoreTimeScale()
    {
        if (!ownsTimeFreeze)
        {
            return;
        }

        Time.timeScale = timeScaleBeforeFreeze;
        ownsTimeFreeze = false;
    }

    private void OnDisable()
    {
        // Never leave the entire game frozen if this object is disabled or a scene
        // changes during the freeze-frame portion of the coroutine.
        RestoreTimeScale();
    }

#if UNITY_EDITOR
    private void OnValidate()
    {
        maxHealth = Mathf.Max(1, maxHealth);

        if (deathEffectOrigin == null)
        {
            deathEffectOrigin = transform;
        }
    }
#endif
}

/// <summary>
/// Implement this on your checkpoint/save/scene controller and assign that
/// component to PlayerHealth.respawnService.
/// </summary>
public interface IPlayerRespawnService
{
    void RespawnPlayer(PlayerHealth player);
}
