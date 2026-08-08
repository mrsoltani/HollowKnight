using System.Collections;
using UnityEngine;
using UnityEngine.UI;

/// <summary>
/// Full-screen transition hook for death/respawn and scene transitions.
/// Uses unscaled time so it remains reliable while gameplay is paused.
/// </summary>
[DisallowMultipleComponent]
public sealed class ScreenFader : MonoBehaviour
{
    [SerializeField] private CanvasGroup canvasGroup;
    [Tooltip("Optional full-screen Image. Disable Raycast Target on it; CanvasGroup controls blocking.")]
    [SerializeField] private Image fadeImage;
    [SerializeField] private Color fadeColor = Color.black;
    [SerializeField] private bool startClear = true;

    private void Reset()
    {
        canvasGroup = GetComponent<CanvasGroup>();
        fadeImage = GetComponent<Image>();
    }

    private void Awake()
    {
        if (canvasGroup == null)
        {
            canvasGroup = GetComponent<CanvasGroup>();
        }

        if (fadeImage != null)
        {
            fadeImage.color = fadeColor;
            fadeImage.raycastTarget = false;
        }

        if (canvasGroup != null && startClear)
        {
            canvasGroup.alpha = 0f;
            canvasGroup.blocksRaycasts = false;
            canvasGroup.interactable = false;
        }
    }

    public IEnumerator FadeOut(float duration)
    {
        yield return FadeTo(1f, duration);
    }

    public IEnumerator FadeIn(float duration)
    {
        yield return FadeTo(0f, duration);
    }

    public void SetImmediate(bool opaque)
    {
        if (canvasGroup == null)
        {
            return;
        }

        canvasGroup.alpha = opaque ? 1f : 0f;
        canvasGroup.blocksRaycasts = opaque;
        canvasGroup.interactable = false;
    }

    private IEnumerator FadeTo(float targetAlpha, float duration)
    {
        if (canvasGroup == null)
        {
            Debug.LogError("ScreenFader needs a CanvasGroup reference.", this);
            yield break;
        }

        float startAlpha = canvasGroup.alpha;
        float elapsed = 0f;
        canvasGroup.blocksRaycasts = true;
        canvasGroup.interactable = false;

        if (duration <= 0f)
        {
            canvasGroup.alpha = targetAlpha;
        }
        else
        {
            while (elapsed < duration)
            {
                elapsed += Time.unscaledDeltaTime;
                float t = Mathf.Clamp01(elapsed / duration);
                canvasGroup.alpha = Mathf.Lerp(startAlpha, targetAlpha, t * t * (3f - 2f * t));
                yield return null;
            }

            canvasGroup.alpha = targetAlpha;
        }

        canvasGroup.blocksRaycasts = targetAlpha > 0.001f;
    }
}
