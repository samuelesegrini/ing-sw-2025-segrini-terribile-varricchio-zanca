# Detailed UML

**Generated. Do not edit.** Run the generator instead:

```bash
python3 tools/uml/generate-detailed.py src/main/java docs/uml/detailed
java -jar /tmp/plantuml.jar -tpng -o . docs/uml/detailed/*.puml
```

One diagram per package, showing every declared type, its fields, its public methods, and
the inheritance between types declared in the same package. Cross-package relations are
left out on purpose: they are what the [high-level package diagram](../high-level/) is for,
and drawing them here would produce a picture nobody can read.

Long member lists are cut at twelve fields and fourteen methods. `Game` and `Ship` are past
that; the source is the authority for the rest.
