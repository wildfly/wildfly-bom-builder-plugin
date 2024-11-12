/*
  ~ Copyright (C) 2013 Red Hat, Inc
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
*/

File file = new File(basedir, "target/bom-pom.xml")
def line
def foundDependency1 = false
def foundDependency2 = false
file.withReader { reader ->
  while ((line = reader.readLine())!=null) {
    if (line.contains("<artifactId>commons-lang3</artifactId>")) {
      foundDependency1 = true
    }
    if (line.contains("<artifactId>commons-text</artifactId>")) {
      foundDependency2 = true
    }
  }
}
if (!foundDependency1) {
  println("VERIFY ERROR: bom-pom.xml does not contain commons-lang3 dependency!")
  return false
}
if (foundDependency2) {
  println("VERIFY ERROR: bom-pom.xml should not contain commons-text dependency!")
  return false
}
