import re

def check_and_parse_method_parameters(input_string):
    """
    Parse MethodParameterIn entries and extract typeFullName, lineNumber, and name.
    """
    # Define a simple regex pattern to extract the relevant fields
    parse_pattern = re.compile(
        r"MethodParameterIn\(\s*"
        r".*?"  # Lazily match everything up to lineNumber
        r"lineNumber\s*=\s*(?:Some\(value\s*=\s*(?P<line>\d+)\)|None),\s*"  # Match lineNumber or None
        r"name\s*=\s*\"(?P<name>[^\"]+)\",\s*"  # Match name
        r".*?"  # Lazily match everything up to typeFullName
        r"typeFullName\s*=\s*\"(?P<type>[^\"]+)\"\s*\)",  # Match typeFullName
        re.DOTALL  # Allow matching across multiple lines
    )

    results = []
    
    for match in parse_pattern.finditer(input_string):
        # Extract matched fields with default values for missing fields
        full_type_name = match.group("type")
        line_number = match.group("line") or "-1"
        name = match.group("name")
        results.append(f"{full_type_name}:{line_number}:{name}")
    
    return results


# Input string
input_string = """
val res5: List[io.shiftleft.codepropertygraph.generated.nodes.MethodParameterIn] = List(
  MethodParameterIn(
    closureBindingId = None,
    code = "this",
    columnNumber = Some(value = 0),
    dynamicTypeHintFullName = IndexedSeq("2.js::program"),
    evaluationStrategy = "BY_VALUE",
    index = 0,
    isVariadic = false,
    lineNumber = Some(value = 1),
    name = "this",
    order = 0,
    possibleTypes = IndexedSeq(),
    typeFullName = "2.js::program"
  ),
  MethodParameterIn(
    closureBindingId = None,
    code = "prop",
    columnNumber = Some(value = 12),
    dynamicTypeHintFullName = IndexedSeq(),
    evaluationStrategy = "BY_VALUE",
    index = 1,
    isVariadic = false,
    lineNumber = Some(value = 1),
    name = "prop",
    order = 1,
    possibleTypes = IndexedSeq("ANY"),
    typeFullName = "ANY"
  ),
  MethodParameterIn(
    closureBindingId = None,
    code = "i",
    columnNumber = Some(value = 18),
    dynamicTypeHintFullName = IndexedSeq(),
    evaluationStrategy = "BY_VALUE",
    index = 2,
    isVariadic = false,
    lineNumber = Some(value = 1),
    name = "i",
    order = 2,
    possibleTypes = IndexedSeq("ANY"),
    typeFullName = "ANY"
  ),
  MethodParameterIn(
    closureBindingId = None,
    code = "p1",
    columnNumber = None,
    dynamicTypeHintFullName = IndexedSeq(),
    evaluationStrategy = "BY_VALUE",
    index = 1,
    isVariadic = false,
    lineNumber = None,
    name = "p1",
    order = 1,
    possibleTypes = IndexedSeq(),
    typeFullName = "ANY"
  )
)
"""

# Parse the parameters
parsed_result = check_and_parse_method_parameters(input_string)

# Display the parsed result
print(parsed_result)

