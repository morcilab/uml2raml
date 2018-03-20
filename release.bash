#!/bin/bash

rm -rf release/*
zip -b templates release/templates.zip *
cp uml2raml/target/uml2raml-*-jar-with-dependencies.jar release/
