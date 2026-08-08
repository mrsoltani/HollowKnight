using System.Collections;
using UnityEngine;

/// <summary>
/// Lightweight camera shake hook that uses unscaled time, so it keeps working
/// during slow motion and death transitions. Put this on a camera child/pivot,
/// not on a transform whose position is continuously overwritten by Cinemachine.
/// </summary>
[DisallowMultipleComponent]
public sealed class CameraShake2D : MonoBehaviour
{
    [SerializeField] private AnimationCurve falloff = AnimationCurve.EaseInOut(0f, 1f, 1f, 0f);

    private Coroutine shakeRoutine;
    private Vector3 originalLocalPosition;

    private void Awake()
    {
        originalLocalPosition = transform.localPosition;
    }

    public void Shake(float duration, float strength)
    {
        if (duration <= 0f || strength <= 0f)
        {
            return;
        }

        if (shakeRoutine != null)
        {
            StopCoroutine(shakeRoutine);
            transform.localPosition = originalLocalPosition;
        }

        shakeRoutine = StartCoroutine(ShakeRoutine(duration, strength));
    }

    private IEnumerator ShakeRoutine(float duration, float strength)
    {
        float elapsed = 0f;

        while (elapsed < duration)
        {
            elapsed += Time.unscaledDeltaTime;
            float normalizedTime = Mathf.Clamp01(elapsed / duration);
            float currentStrength = strength * falloff.Evaluate(normalizedTime);
            Vector2 offset = Random.insideUnitCircle * currentStrength;
            transform.localPosition = originalLocalPosition + new Vector3(offset.x, offset.y, 0f);
            yield return null;
        }

        transform.localPosition = originalLocalPosition;
        shakeRoutine = null;
    }

    private void OnDisable()
    {
        transform.localPosition = originalLocalPosition;
        shakeRoutine = null;
    }
}
