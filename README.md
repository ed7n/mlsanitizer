# MLSanitizer

[[**Homepage**](https://ed7n.github.io/mlsanitizer)]

## Building

    $ cd release && jar -x -f ../lib/edjc.jar eden && cd ..
    $ javac --class-path lib/edjc.jar -d release --release 8 --source-path src src/eden/mlsanitizer/MLSanitizer.java && jar -c -f release/mlsanitizer.jar -e eden.mlsanitizer.MLSanitizer -C release eden

## Formatting

    $ prettier --write '**/*.java'
