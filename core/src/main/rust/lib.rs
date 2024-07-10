use std::fmt::{Debug, Display};

use jni::JNIEnv;
use tracing::warn;

mod audio_util;
mod discord_bot;
mod logging;
mod runtime;

trait DisplayDebugThrow: Display + Debug {
    fn throw(&self, env: &mut JNIEnv<'_>) {
        let _ = env.throw(("java/lang/Exception", format!("{self:#}")));
    }
}

impl<T: Display + Debug> DisplayDebugThrow for T {}

trait ResultExt<T> {
    /// Discards the result if it is `Ok`, otherwise throws the error.
    fn discard_or_throw(self, env: &mut JNIEnv<'_>);
    /// Unwraps the result if it is `Ok`, otherwise throws the error and returns `value_on_throw`.
    fn unwrap_or_throw(self, env: &mut JNIEnv<'_>, value_on_throw: T) -> T;
}

impl<T, E: Display + Debug> ResultExt<T> for Result<T, E> {
    fn discard_or_throw(self, env: &mut JNIEnv<'_>) {
        if let Err(e) = self {
            e.throw(env);
        }
    }

    fn unwrap_or_throw(self, env: &mut JNIEnv<'_>, value_on_throw: T) -> T {
        match self {
            Ok(r) => r,
            Err(e) => {
                e.throw(env);
                value_on_throw
            }
        }
    }
}


#[no_mangle]
pub extern "system" fn Java_dev_naturecodevoid_voicechatdiscord_Core_initializeNatives<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
) {
    logging::ensure_init();
    info!("Initializing rustls");
    if rustls::crypto::ring::default_provider().install_default().is_err() {
        warn!("rustls already has a default provider. This is probably fine");
    }
}

#[no_mangle]
pub extern "system" fn Java_dev_naturecodevoid_voicechatdiscord_Core_shutdownNatives<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
) {
    RUNTIME.shutdown();
}
