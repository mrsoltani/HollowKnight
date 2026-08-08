using System.Collections;
using UnityEngine;

/// <summary>
/// Put on one-shot VFX prefabs such as ShadowScream. It reads the current
/// Animator state's duration and destroys the spawned object afterward.
/// </summary>
[DisallowMultipleComponent]
public sealed class DestroyAfterAnimation : MonoBehaviour
{
    [SerializeField] private Animator animator;
    [SerializeField, Min(0f)] private float fallbackLifetime = 2f;
    [SerializeField, Min(0f)] private float extraLifetime = 0.1f;

    private void Reset()
    {
        animator = GetComponentInChildren<Animator>();
    }

    private IEnumerator Start()
    {
        // Let the Animator enter its default ShadowScream state before reading it.
        yield return null;

        float lifetime = fallbackLifetime;

        if (animator != null && animator.runtimeAnimatorController != null)
        {
            AnimatorStateInfo state = animator.GetCurrentAnimatorStateInfo(0);
            if (state.length > 0f)
            {
                lifetime = state.length;
            }
        }

        Destroy(gameObject, lifetime + extraLifetime);
    }
}
