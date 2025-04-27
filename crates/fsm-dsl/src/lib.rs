// Placeholder for the FSM DSL library implementation.

pub mod parser;
pub mod ast;
pub mod validation;
// pub mod generator; // Uncomment when generator module is added

pub fn add(left: usize, right: usize) -> usize {
    left + right
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn it_works() {
        let result = add(2, 2);
        assert_eq!(result, 4);
    }
} 