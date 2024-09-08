#!/bin/bash

javadoc \
    -Werror \
    -d . \
    -notree \
    -noindex \
    ../MedianState.java
