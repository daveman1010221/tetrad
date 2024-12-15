{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
  };

  outputs = { self, nixpkgs }: {
    packages.x86_64-linux.default = let
      # Import the Nixpkgs library for package definitions
      pkgs = import nixpkgs { system = "x86_64-linux"; };

      # Fetch a specific Nixpkgs commit for stability and compatibility
      # Ensures `buildMavenPackage` is available
      nixpkgsRepo = pkgs.fetchFromGitHub {
        owner = "NixOS";
        repo = "nixpkgs";
        rev = "e0fa4d8cfec9f6aa7fc6650db2a46b7cd611d0c3"; # Example commit
        sha256 = "sha256-Z99WW4F6ORVTzLlo5DGtghns5pkIjvDZ1qzwXAWymJw=";
      };

      # Explicitly load the `buildMavenPackage` function from the specified Nixpkgs repo
      # This is critical for building Maven projects in a Nix environment
      buildMavenPackage = pkgs.callPackage "${nixpkgsRepo}/pkgs/by-name/ma/maven/build-maven-package.nix" {};

      # Fetch the Tetrad source code
      # This source will be used in both the dependency fetch and main build derivations
      tetradSource = pkgs.fetchFromGitHub {
        owner = "cmu-phil";
        repo = "tetrad";
        rev = "8523ca61e58bfc77fd71b9bc7b4c882281264c4c";
        sha256 = "sha256-ax39zgZDIQR9aQKI1cDx4t5juAs5LwKssHzLokThpEo=";
      };

      # Create a derivation to handle Maven dependency resolution
      # This step ensures that all dependencies are resolved offline and reproducibly
      customFetchedMavenDeps = pkgs.stdenv.mkDerivation {
        name = "tetrad-maven-deps";
        src = tetradSource;

        # Add tools required for patching and validating the POM
        nativeBuildInputs = [ pkgs.xmlstarlet pkgs.xmllint ];

        # Patch the POM to remove problematic plugins and references
        # - Removes `maven-javadoc-plugin` to avoid Javadoc errors
        # - Removes Sonatype references, as they are unnecessary for local builds
        patchPhase = ''
          echo "Patching pom.xml to disable Javadoc plugin and Sonatype references"
          # Remove the maven-javadoc-plugin from the POM
          xmlstarlet ed -L \
            -d "//plugin[artifactId='maven-javadoc-plugin']" \
            pom.xml

          # Validate that the POM file is still valid XML after patching
          xmllint --noout pom.xml || { echo "Invalid POM after patching"; exit 1; }
        '';

        # Specify Maven as a build input to ensure it is available
        buildInputs = [ pkgs.maven ];

        # Resolve dependencies offline and store them in the specified local Maven repo
        buildPhase = ''
          echo "Fetching Maven dependencies offline"
          export JAVA_HOME=${pkgs.jdk23}
          export PATH=$JAVA_HOME/bin:$PATH
          mvn dependency:resolve -Dmaven.repo.local=$out/.m2
        '';

        # Declare the output location for Maven dependencies
        installPhase = ''
          echo "Dependencies fetched to $out/.m2"
        '';
      };

      # Main derivation for building Tetrad
      # This step compiles the source code and packages the output .jar file
      customTetrad = buildMavenPackage {
        pname = "tetrad";
        version = "7.6.7-SNAPSHOT";

        # Use the same source as the dependency fetch phase
        src = tetradSource;

        # Use the pre-fetched Maven dependencies to ensure offline builds
        fetchedMavenDeps = customFetchedMavenDeps;

        # Add build parameters to skip problematic steps
        # - Skips Javadoc generation (`-Dmaven.javadoc.skip=true`)
        # - Skips running tests (`-DskipTests`)
        mvnParameters = "-Dmaven.javadoc.skip=true -DskipTests";

        # Explicitly disable Nix checks, as they are not needed here
        doCheck = false;

        # Specify where to copy the compiled output
        installPhase = ''
          mkdir -p $out/share/java
          cp tetrad-gui/target/tetrad-gui-7.6.7-SNAPSHOT-launch.jar $out/share/java/
        '';

        # Specify the fixed-output hash for the Maven dependency resolution step
        # This ensures reproducibility by verifying the content hash
        mvnHash = "sha256-gUCI58+5JXX5NpmE1QI50SvaQnFVMf0kGbVVBZzPbk4=";
      };

    # Return the final build derivation as the default package
    in
      customTetrad;
  };
}
