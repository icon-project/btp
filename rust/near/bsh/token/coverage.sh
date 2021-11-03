export RUSTFLAGS="-Zinstrument-coverage"
export LLVM_PROFILE_FILE="token_bsh-test-%p-%m.profraw"
cargo test --features testable
grcov . --ignore "**/tests/*" --binary-path ../../target/debug -s ./ -t lcov --ignore-not-existing -o ./lcov.info
grcov . --ignore "**/tests/*" --binary-path ../../target/debug -s ./ -t html -o ./coverage
rm *.profraw