/*
  ~ Copyright The WildFly Authors
  ~ SPDX-License-Identifier: Apache-2.0
*/

File file = new File(basedir, "bom/target/bom-pom.xml")
def line
def foundDependency1 = false
def foundDependency2 = false
def foundDependency3 = false
def foundDependency2Version = false
def foundDependency3Version = false
file.withReader { reader ->
  while ((line = reader.readLine())!=null) {
    if (line.contains("<artifactId>module1</artifactId>")) {
      foundDependency1 = true
    }
    if (line.contains("<artifactId>assertj-core</artifactId>")) {
      foundDependency2 = true
    }
    if (line.contains("<artifactId>byte-buddy</artifactId>")) {
      foundDependency3 = true
    }
    if (line.contains("<version.org.assertj>3.27.6</version.org.assertj>")) {
      foundDependency2Version = true
    }
    if (line.contains("<version.net.bytebuddy>1.18.10</version.net.bytebuddy>")) {
      foundDependency3Version = true
    }
  }
}
if (!foundDependency1) {
  println("VERIFY ERROR: bom-pom.xml does not contain module1 dependency!")
  return false
}
if (!foundDependency2) {
  println("VERIFY ERROR: bom-pom.xml does not contain assertj-core dependency, a module1 dependency which is also managed in the builder!")
  return false
}
if (!foundDependency3) {
  println("VERIFY ERROR: bom-pom.xml does not contain byte-buddy dependency, a transitive of assertj-core that is excluded by module1, but which is managed in the builder and so must be added to the BOMs, otherwise version is not the managed one when using assertj-core!")
  return false
}
if (!foundDependency2Version) {
  println("VERIFY ERROR: bom-pom.xml assertj-core version is not the builder's managed one!")
  return false
}
if (!foundDependency3Version) {
  println("VERIFY ERROR: bom-pom.xml byte-buddy version is not the builder's managed one!")
  return false
}