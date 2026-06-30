package com.axguard.sdk.internal.utils

// Re-read on every access: ART can vary this at runtime (thermal throttling,
// big.LITTLE core parking), and a cached stale value oversizes thread pools.
internal val availableProcessors: Int
    get() = Runtime.getRuntime().availableProcessors()
