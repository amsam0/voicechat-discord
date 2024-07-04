use std::fmt::{Debug, Display};

use jni::JNIEnv;

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
