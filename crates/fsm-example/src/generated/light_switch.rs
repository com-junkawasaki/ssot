#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum State {
    ///The light is off.
    Off,
    ///The light is on.
    On,
}
///Turns the light on with a specific brightness.
#[derive(Debug, Clone, PartialEq)]
pub struct TurnOnEventPayload {
    ///Brightness level (0-255).
    pub brightness: u8,
}
#[derive(Debug, Clone, PartialEq)]
pub enum Event {
    ///Toggles the light state.
    Toggle,
    ///Turns the light on with a specific brightness.
    TurnOn(TurnOnEventPayload),
    ///Turns the light off.
    TurnOff,
}
#[derive(Debug, Clone, PartialEq)]
pub struct LightSwitch {
    pub current_state: State,
}
/// Trait defining the required guard, action, entry, and exit callbacks for the state machine.
pub trait LightSwitchCallbacks {
    fn activate_light(&mut self, event: &Event);
    fn activate_light_specific(&mut self, event: &TurnOnEventPayload);
    fn deactivate_light_specific(&mut self, event: &Event);
    fn deactivate_light(&mut self, event: &Event);
}
impl LightSwitch {
    /// Creates a new instance of the state machine in its initial state.
    pub fn new() -> Self {
        Self {
            current_state: State::Off,
        }
    }
    /// Processes an event and attempts to transition the state machine.
    /// Requires `Self` to implement the `#callbacks_trait_name` trait if guards or actions are defined.
    /// Returns the new state machine instance if successful (transition occurred, action ran).
    /// Returns the *original* state machine instance `Ok(self)` if a guard prevents the transition.
    /// Returns an `Err` for unhandled state/event combinations.
    pub fn on_event(self, event: Event) -> Result<Self, String>
    where
        Self: LightSwitchCallbacks,
    {
        match (&self.current_state, &event) {
            (State::Off, Event::Toggle) => {
                let mut next_state_machine = self.clone();
                next_state_machine.current_state = State::On;
                next_state_machine.activate_light(&event);
                Ok(next_state_machine)
            }
            (State::On, Event::Toggle) => {
                let mut next_state_machine = self.clone();
                next_state_machine.current_state = State::Off;
                next_state_machine.deactivate_light(&event);
                Ok(next_state_machine)
            }
            (State::Off, Event::TurnOn(payload)) => {
                let mut next_state_machine = self.clone();
                next_state_machine.current_state = State::On;
                next_state_machine.activate_light_specific(payload);
                Ok(next_state_machine)
            }
            (State::On, Event::TurnOff) => {
                let mut next_state_machine = self.clone();
                next_state_machine.current_state = State::Off;
                next_state_machine.deactivate_light_specific(&event);
                Ok(next_state_machine)
            }
            _ => Ok(self.clone()),
        }
    }
    /// Returns the current state.
    pub fn current_state(&self) -> &State {
        &self.current_state
    }
}
impl Default for LightSwitch {
    fn default() -> Self {
        Self::new()
    }
}
