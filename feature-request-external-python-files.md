# Feature Request: External Python Files for KSML Functions

## Problem

Writing Python code inside YAML files is painful:
- **Indentation errors** when copy-pasting code
- **No IDE support** (no autocomplete, syntax highlighting, or debugging)
- **Hard to read** long functions in YAML format

## Proposed Solution

Let users write Python code in `.py` files instead of inline YAML.

### Current Way (Inline - Hard to Work With)
```yaml
functions:
  process:
    type: valueTransformer
    code: |
      # This code has no IDE support :(
      import json
      data = my_store.get(key)  # No autocomplete for 'get'
      if data:
          result = json.loads(data)  # Easy to mess up indentation
      else:
          result = {"count": 0}
      result["count"] += 1
      my_store.put(key, json.dumps(result))
      return f"Count: {result['count']}"
```

### New Way (External File - Full IDE Support)
```yaml
functions:
  process:
    type: valueTransformer
    file: "process.py"  # Reference external Python file
    stores: [my_store]
```

```python
# process.py - Full IDE support! 
import json

data = my_store.get(key)
if data:
    result = json.loads(data)
else:
    result = {"count": 0}
    
result["count"] += 1
my_store.put(key, json.dumps(result))
return f"Count: {result['count']}"
```

## IDE Autocomplete Support

KSML can generate a helper file for IDE autocomplete:

```bash
ksml generate-stubs processor.yaml
# Creates: ksml_stores.py with all your stores
```

Example of generated `ksml_stores.py`:
```python
# Auto-generated from processor.yaml - DO NOT EDIT
# This file provides IDE support for KSML state stores

class StateStore:
    """KSML State Store interface for IDE support"""
    def get(self, key: str) -> str | None:
        """Get value for key from state store"""
        pass
    
    def put(self, key: str, value: str) -> None:
        """Store key-value pair in state store"""
        pass
    
    def delete(self, key: str) -> None:
        """Delete key from state store"""
        pass

# State stores from your YAML definition
user_session_store = StateStore()
circuit_breaker_store = StateStore()
metrics_cache_store = StateStore()
```

Then users can optionally import for autocomplete:
```python
# process.py
from ksml_stores import user_session_store  # Optional - just for IDE autocomplete!

# Full IDE support: autocomplete shows get(), put(), delete() methods
session_data = user_session_store.get(key)
if session_data:
    data = json.loads(session_data)
    data["count"] += 1
    user_session_store.put(key, json.dumps(data))
```

## Benefits

✅ **No more indentation errors**  
✅ **Full IDE support** (autocomplete, debugging, linting)  
✅ **Easier to read and maintain**  
✅ **Can unit test Python functions**  
✅ **Backwards compatible** (inline code still works)

## Global Code in External Files

Allow shared imports and helper functions in separate Python files:

### Current Way (Inline Global Code)
```yaml
functions:
  process_with_retry:
    type: valueTransformer
    globalCode: |
      import json
      import time
      import random
      
      MAX_RETRIES = 3
      BACKOFF_MS = 1000
      
      def calculate_backoff(attempt):
          delay = BACKOFF_MS * (2 ** attempt)
          jitter = random.randint(0, delay // 4)
          return delay + jitter
          
      def should_retry(error_type):
          return error_type in ["timeout", "connection_error"]
    code: |
      # Main processing logic using global functions
      result = process_message(value)
```

### New Way (External Global File)
```yaml
functions:
  process_with_retry:
    type: valueTransformer
    globalFile: "common/helpers.py"  # Reference external file for globals
    file: "process_retry.py"         # Main processing logic
```

```python
# common/helpers.py - Shared across multiple functions
import json
import time
import random

MAX_RETRIES = 3
BACKOFF_MS = 1000

def calculate_backoff(attempt):
    delay = BACKOFF_MS * (2 ** attempt)
    jitter = random.randint(0, delay // 4)
    return delay + jitter
    
def should_retry(error_type):
    return error_type in ["timeout", "connection_error"]
```

### Hybrid Approach (Both External and Inline)
```yaml
functions:
  process:
    type: valueTransformer
    globalFile: "common/imports.py"  # Common imports
    globalCode: |                    # Function-specific constants
      CUSTOM_THRESHOLD = 100
      LOCAL_TIMEOUT = 5000
    file: "process.py"
```

## Simple Example

**Before:** Everything crammed in YAML
```yaml
functions:
  validate:
    type: predicate
    code: |
      parts = value.split(":")
      if len(parts) != 3:
          return False
      try:
          amount = float(parts[2])
          return amount > 0
      except:
          return False
```

**After:** Clean YAML + Testable Python
```yaml
functions:
  validate:
    type: predicate
    file: "validate.py"
```

```python
# validate.py
def validate_message(value):
    parts = value.split(":")
    if len(parts) != 3:
        return False
    try:
        amount = float(parts[2])
        return amount > 0
    except:
        return False

# Now you can even unit test it!
return validate_message(value)
```

## Summary

Allow `.py` files instead of inline code. Same KSML functionality, better developer experience.