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

File file = new File(basedir, "builder-module/target/bom-pom.xml")
def line
def foundDependency = false
def foundParentArtifactid = false
def foundLicense = false
file.withReader { reader ->
  while ((line = reader.readLine())!=null) {
    if (line.contains("<artifactId>commons-lang3</artifactId>")) {
      foundDependency = true
    } 
    if (line.contains("<artifactId>alternative-parent</artifactId>")) {
      foundParentArtifactid = true;
    }
    if (line.contains("<url>http://repository.jboss.org/licenses/apache-2.0.txt</url>")) {
      foundLicense = true
    } 
  }
}
if (!foundDependency) {
  println("VERIFY ERROR: bom-pom.xml does not contain commons-lang3 dependency!")
  return false
}
if (!foundParentArtifactid) {
  println("VERIFY ERROR: bom-pom.xml does not contain alternative-parent artifactId!")
  return false
}
if (!foundLicense) {
  println("VERIFY ERROR: bom-pom.xml does not contain Apache license URL!")
  return false
}


