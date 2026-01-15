{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
  };

  outputs = { self, nixpkgs, ... }: {
    packages.x86_64-linux.default = let
      pkgs = import nixpkgs { system = "x86_64-linux"; };
      buildMavenPackage = pkgs.callPackage "${nixpkgs}/pkgs/by-name/ma/maven/build-maven-package.nix" {};

      # Fetch Maven dependencies deterministically
      customFetchedMavenDeps = pkgs.stdenv.mkDerivation {
        name = "tetrad-maven-deps";
        src = self;

        outputHashMode = "recursive";
        outputHashAlgo = "sha256";
        outputHash = "sha256-foa+oshSYPOZmNiT3xR6HABgJDVCUJIU0qiin2GDPJI=";

        nativeBuildInputs = [ pkgs.xmlstarlet pkgs.xmllint pkgs.maven pkgs.jdk25 ];

	patchPhase = ''
	  echo "Patching pom.xml to disable Javadoc plugin and Sonatype Central publishing plugin"
	  xmlstarlet ed -L \
	    -d "/project/build/plugins/plugin[groupId='org.apache.maven.plugins' and artifactId='maven-javadoc-plugin']" \
	    -d "/project/build/plugins/plugin[groupId='org.sonatype.central' and artifactId='central-publishing-maven-plugin']" \
	    pom.xml
	  xmllint --noout pom.xml
	'';

        buildPhase = ''
          echo "Fetching Maven dependencies offline"
          export JAVA_HOME=${pkgs.jdk25}
          export PATH=$JAVA_HOME/bin:$PATH
	  mvn dependency:resolve -Dmaven.repo.local=$out/.m2 -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true
        '';

        installPhase = ''
          echo "Dependencies fetched to $out/.m2"
        '';
      };

      customTetrad = buildMavenPackage {
        pname = "tetrad";
        version = self.shortRev or "dirty";
        src = self;
        fetchedMavenDeps = customFetchedMavenDeps;
	mvnParameters = "-DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true";
        doCheck = false;

        installPhase = ''
          mkdir -p $out/share/java
          cp tetrad-gui/target/tetrad-gui-*-launch.jar $out/share/java/
        '';

        mvnHash = "sha256-foa+oshSYPOZmNiT3xR6HABgJDVCUJIU0qiin2GDPJI=";
      };

    in
      customTetrad;
  };
}
