val x = if (2 > 1) <caret>3 else 4

// TYPE: 3 -> <html>Int</html>
// TYPE: if (2 > 1) 3 else 4 -> <html>Int</html>
// TYPE: val x = if (2 > 1) 3 else 4 -> <html>Int</html>
