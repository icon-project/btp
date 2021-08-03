use crate::common;
use kitten::*;

pub struct Calculator {}
impl Calculator {
    fn add(&self, a: i32, b: i32) -> i32 {
        a + b
    }
}

#[cfg(feature = "integration-tests")]
#[test]
fn calculator_can_add_numbers() {
    Test::scenario()
        .given(a_calculator)
        .when(adding_1_and_2)
        .then(the_answer_is_3);
}

fn a_calculator() -> Calculator {
    Calculator {}
}

fn adding_1_and_2(calculator: &Calculator) -> i32 {
    calculator.add(1, 2)
}

fn the_answer_is_3(the_answer: &i32) -> () {
    assert_eq!(3, the_answer.clone());
}
