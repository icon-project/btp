mod bmc_event;
pub use bmc_event::BmcEvent;

#[macro_export]
macro_rules! emit_message {
    ($self: ident, $event: ident, $($opt:expr),+) => {
        $self.$event.amend_event($($opt),+)
    }
}