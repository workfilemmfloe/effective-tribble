fun foo() {
  return
  return 1
  return (@a 1)

  return@
  return@ 1
  return@ (@ 1)
  return@ @ 1

  return@a
  return@a 1
  return@a (@a 1)
  return@a @a 1

  return@@
  return@@ 1
  return@@ (@@ 1)
  return@@ @@ 1

  @a return@@
  @a return@@ 1
  @a return@@ (@@ 1)
  @a return@@ @@ 1

  break
  break@
  break@a
  break@@

  continue
  continue@
  continue@a
  continue@@

  a.filter @f{
      if (1) return
      return@f true
  }

  a.filter @{
      if (1) return
      return@ true
  }

  a.filter @@{
      if (1) return
      return@@ true
  }

  this
  this@
  this@a
  this@@

  super<A>
  super<A>@
  super<A>@a
  super<A>@@
}