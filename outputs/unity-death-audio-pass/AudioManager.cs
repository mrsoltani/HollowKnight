using UnityEngine;
using UnityEngine.Audio;

/// <summary>
/// Central SFX owner. PlayerHealth requests semantic cues; callers never need
/// to know which clips or layers form a cue.
/// </summary>
[DisallowMultipleComponent]
public sealed class AudioManager : MonoBehaviour
{
    public static AudioManager Instance { get; private set; }

    [Header("Mixer Routing")]
    [SerializeField] private AudioMixerGroup sfxMixerGroup;

    [Header("Player Damage")]
    [SerializeField] private AudioClip[] playerDamageClips;
    [SerializeField, Range(0f, 1f)] private float playerDamageVolume = 0.9f;
    [SerializeField, Range(0f, 0.25f)] private float playerDamagePitchVariance = 0.06f;

    [Header("Player Death Layers")]
    [Tooltip("Assign Hero Death V2.mp3")]
    [SerializeField] private AudioClip playerDeathMain;
    [Tooltip("Assign Hero Death Extra Details.mp3")]
    [SerializeField] private AudioClip playerDeathDetails;
    [SerializeField, Range(0f, 1f)] private float playerDeathMainVolume = 1f;
    [SerializeField, Range(0f, 1f)] private float playerDeathDetailsVolume = 0.85f;

    [Header("Runtime Voice Pool")]
    [SerializeField, Min(4)] private int initialVoiceCount = 12;

    private AudioSource[] voices;
    private int nextVoiceIndex;
    private int lastDamageClipIndex = -1;

    private void Awake()
    {
        if (Instance != null && Instance != this)
        {
            Destroy(gameObject);
            return;
        }

        Instance = this;
        DontDestroyOnLoad(gameObject);
        BuildVoicePool();
    }

    private void BuildVoicePool()
    {
        voices = new AudioSource[Mathf.Max(4, initialVoiceCount)];

        for (int i = 0; i < voices.Length; i++)
        {
            AudioSource source = gameObject.AddComponent<AudioSource>();
            source.playOnAwake = false;
            source.loop = false;
            source.spatialBlend = 0f;
            source.outputAudioMixerGroup = sfxMixerGroup;
            voices[i] = source;
        }
    }

    public void PlayPlayerDamage()
    {
        AudioClip clip = GetNonRepeatingRandomClip(playerDamageClips, ref lastDamageClipIndex);
        if (clip == null)
        {
            return;
        }

        float pitch = Random.Range(1f - playerDamagePitchVariance, 1f + playerDamagePitchVariance);
        PlayOneShot(clip, playerDamageVolume, pitch);
    }

    /// <summary>
    /// Plays both death clips in the same frame on separate AudioSources.
    /// Separate voices are required so neither layer interrupts the other.
    /// </summary>
    public void PlayPlayerDeath()
    {
        // Stop movement loops or other persistent player channels here if your
        // full AudioManager owns them (footsteps, wall slide, focus loop, etc.).
        PlayOneShot(playerDeathMain, playerDeathMainVolume, 1f);
        PlayOneShot(playerDeathDetails, playerDeathDetailsVolume, 1f);
    }

    public void PlayOneShot(AudioClip clip, float volume = 1f, float pitch = 1f)
    {
        if (clip == null)
        {
            return;
        }

        AudioSource source = AcquireVoice();
        source.pitch = pitch;
        source.PlayOneShot(clip, Mathf.Clamp01(volume));
    }

    private AudioSource AcquireVoice()
    {
        // Prefer an idle voice so active clips are not interrupted.
        for (int offset = 0; offset < voices.Length; offset++)
        {
            int index = (nextVoiceIndex + offset) % voices.Length;
            if (!voices[index].isPlaying)
            {
                nextVoiceIndex = (index + 1) % voices.Length;
                return voices[index];
            }
        }

        // Pool exhausted: steal the oldest round-robin voice. In a larger game,
        // replace this with priority-based voice limiting.
        AudioSource fallback = voices[nextVoiceIndex];
        fallback.Stop();
        nextVoiceIndex = (nextVoiceIndex + 1) % voices.Length;
        return fallback;
    }

    private static AudioClip GetNonRepeatingRandomClip(AudioClip[] clips, ref int previousIndex)
    {
        if (clips == null || clips.Length == 0)
        {
            return null;
        }

        if (clips.Length == 1)
        {
            previousIndex = 0;
            return clips[0];
        }

        int index;
        do
        {
            index = Random.Range(0, clips.Length);
        }
        while (index == previousIndex);

        previousIndex = index;
        return clips[index];
    }

#if UNITY_EDITOR
    private void OnValidate()
    {
        initialVoiceCount = Mathf.Max(4, initialVoiceCount);
    }
#endif
}
