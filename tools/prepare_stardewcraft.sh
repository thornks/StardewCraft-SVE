#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source_dir="${1:-${repo_root}/.gradle/stardewcraft-source}"
output_jar="${2:-${repo_root}/.gradle/release-deps/stardewcraft.jar}"
properties="${repo_root}/gradle.properties"

property() {
    sed -n "s/^$1=//p" "${properties}" | tail -n 1 | tr -d '\r'
}

version="$(property stardewcraft_version)"
commit="$(property stardewcraft_source_commit)"

if [[ -z "${version}" || -z "${commit}" ]]; then
    echo "Missing StardewCraft version or source commit in gradle.properties" >&2
    exit 1
fi

if [[ ! -d "${source_dir}/.git" ]]; then
    rm -rf "${source_dir}"
    git clone --filter=blob:none --no-checkout \
        https://github.com/ChangQingElysium/Starfield-Pastoral.git "${source_dir}"
fi

git -C "${source_dir}" fetch --depth=1 origin "${commit}"
git -C "${source_dir}" checkout --detach --force "${commit}"

wrapper_properties="${source_dir}/gradle/wrapper/gradle-wrapper.properties"
gradle_sha256="72f44c9f8ebcb1af43838f45ee5c4aa9c5444898b3468ab3f4af7b6076c5bc3f"
sed -i 's/^networkTimeout=.*/networkTimeout=60000/' "${wrapper_properties}"
if grep -q '^distributionSha256Sum=' "${wrapper_properties}"; then
    sed -i "s/^distributionSha256Sum=.*/distributionSha256Sum=${gradle_sha256}/" "${wrapper_properties}"
else
    printf '\ndistributionSha256Sum=%s\n' "${gradle_sha256}" >> "${wrapper_properties}"
fi
printf '\nrootProject.name = "stardewcraft"\n' >> "${source_dir}/settings.gradle"

# The pinned StardewCraft commit declares Jade as a flatDir compile dependency.
# Keep that otherwise-ignored input reproducible for a clean CI checkout.
jade_file="${source_dir}/libs/Jade-1.21.1-NeoForge-15.10.4.jar"
jade_sha256="54856c2405f1302023991e315a5522eea3d60a6823b36286a5c4a102ba476120"
if [[ ! -f "${jade_file}" ]] || ! echo "${jade_sha256}  ${jade_file}" | sha256sum --check --status; then
    mkdir -p "${source_dir}/libs"
    curl --fail --location --retry 3 --silent --show-error \
        --output "${jade_file}" \
        "https://cdn.modrinth.com/data/nvQzSEkH/versions/VGRMP69T/Jade-1.21.1-NeoForge-15.10.4.jar"
fi
echo "${jade_sha256}  ${jade_file}" | sha256sum --check --status

build_succeeded=false
for attempt in 1 2 3; do
    if (
        cd "${source_dir}"
        ./gradlew clean jar --no-daemon --console=plain
    ); then
        build_succeeded=true
        break
    fi
    echo "StardewCraft build attempt ${attempt} failed" >&2
done
if [[ "${build_succeeded}" != true ]]; then
    echo "Unable to build pinned StardewCraft dependency after 3 attempts" >&2
    exit 1
fi

built_jar="${source_dir}/build/libs/stardewcraft-${version}.jar"
if [[ ! -f "${built_jar}" ]]; then
    echo "Pinned StardewCraft build did not produce ${built_jar}" >&2
    exit 1
fi

mkdir -p "$(dirname "${output_jar}")"
cp "${built_jar}" "${output_jar}"
echo "Prepared StardewCraft ${version} from ${commit}: ${output_jar}"
