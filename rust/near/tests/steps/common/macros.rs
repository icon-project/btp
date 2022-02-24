#[macro_export]
macro_rules! user_call {
    ($method: ident; $($user: ident),*) => {
        paste::paste! {
                $(
                    pub static [<$user _INVOKES_ $method>]: fn(Context) -> Context = |mut context: Context| {
                        context
                            .pipe([<THE_TRANSACTION_IS_SIGNED_BY_ $user>])
                            .pipe([<USER_INVOKES_ $method>])
                    };
                )*
        }
    };
}
