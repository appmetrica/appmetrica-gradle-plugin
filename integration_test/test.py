import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
import zipfile
import shutil

plugin_path = "."

errors = []

parser = argparse.ArgumentParser(prog="Test appmetrica-gradle-plugin")
parser.add_argument("--gradle_wrapper_version")
parser.add_argument("--sample_path")
parser.add_argument("--agp_version", nargs="+")

args = parser.parse_args()

def format_args(args={}):
    result = []
    for arg in args:
        result.append(f"-P{arg}={args[arg]}")
    return result


def gradle_wrapper(cwd, args={}):
    print("[test.py] Running wrapper", flush=True)
    gradle_wrapper_properties = f"{cwd}/gradle/wrapper/gradle-wrapper.properties"
    wrapper_version = args["wrapper.version"]
    with open(gradle_wrapper_properties, "w") as f:
        f.write("distributionBase=GRADLE_USER_HOME\n")
        f.write("distributionPath=wrapper/dists\n")
        f.write(f"distributionUrl=https://services.gradle.org/distributions/gradle-{wrapper_version}.zip\n")
        f.write("zipStoreBase=GRADLE_USER_HOME\n")
        f.write("zipStorePath=wrapper/dists\n")
    return_code = subprocess.call(
        " ".join([gradle_wrapper_bin(), "wrapper"] + format_args(args)),
        shell=True,
        cwd=cwd
    )

    if return_code != 0:
        raise Exception("Cannot update wrapper")


def gradle_wrapper_bin():
    return "./gradlew"


def show_gradle_version(cwd):
    print("[test.py] Showing gradle version", flush=True)
    return_code = subprocess.call(
        " ".join([gradle_wrapper_bin(), "-v"]),
        shell=True,
        cwd=cwd
    )

    if return_code != 0:
        raise Exception("Cannot show gradle wrapper version")


def assemble_and_upload_mappings(cwd, build_type, args={}, expect_success=True):
    print(f"[test.py] Running assemble{build_type} for the first time", flush=True)
    return_code = subprocess.call(
        " ".join([gradle_wrapper_bin(), f"clean assemble{build_type} -i -s"] + format_args(args)),
        shell=True,
        cwd=cwd
    )

    if expect_success:
        if return_code != 0:
            raise Exception(f"Failed to assemble {build_type} apk and upload mappings")
    else:
        if return_code == 0:
            raise Exception(f"Expected failure to assemble {build_type} apk and upload mappings")

    # Run again to verify configuration cache reuse works correctly
    print(f"[test.py] Running assemble{build_type} a second time", flush=True)
    return_code = subprocess.call(
        " ".join([gradle_wrapper_bin(), f"assemble{build_type} -i -s"] + format_args(args)),
        shell=True,
        cwd=cwd
    )

    if expect_success:
        if return_code != 0:
            raise Exception(f"Failed to assemble {build_type} with configuration cache reuse")
    else:
        if return_code == 0:
            raise Exception(f"Expected failure to assemble {build_type} apk and upload mappings")


def publish_plugin_to_maven_local(cwd, args={}):
    print("[test.py] Publishing plugin to maven local", flush=True)
    return_code = subprocess.call(
        " ".join([gradle_wrapper_bin(), "clean publishPluginsToMavenLocal -i -s"] + format_args(args)),
        shell=True,
        cwd=cwd
    )

    if return_code != 0:
        raise Exception("Failed to publish plugin")


def check_info_file_to_upload(zip_file):
    print("[test.py] Checking info file to upload", flush=True)
    info_to_upload = json.loads(zip_file.read("info.txt").decode("utf-8"))
    print(f"[test.py] Read info file {info_to_upload}", flush=True)
    necessary_keys = ["build_id", "version_name", "version_code", "mapping_type", "split_version_codes"]
    info_contains_necessary_keys = all(
        file in info_to_upload.keys()
        for file in necessary_keys
    )
    print(f"[test.py] Info file contains necessary keys: {info_contains_necessary_keys}", flush=True)
    info_contains_necessary_values = all(
        [
            info_to_upload["version_name"] == "1.0",
            info_to_upload["version_code"] == 1,
            info_to_upload["mapping_type"] == "PROGUARD",
            info_to_upload["split_version_codes"] == [1]
        ]
    )
    print(f"[test.py] Info file contains necessary values: {info_contains_necessary_values}", flush=True)
    if not (info_contains_necessary_keys and info_contains_necessary_values):
        raise Exception(f"Info file should contain keys '{necessary_keys}' with proper values")


def check_necessary_files_to_upload(zip_file, necessary_files):
    print(f"[test.py] Checking necessary files to upload {necessary_files}", flush=True)
    zip_contains_necessary_files = all(
        file in zip_file.namelist()
        for file in necessary_files
    )
    print(f"[test.py] Zip file contains necessary files: {zip_contains_necessary_files}", flush=True)
    if not zip_contains_necessary_files:
        raise Exception(f"Zip file should contain {necessary_files}")


def check_symbol_files_to_upload(zip_file_name):
    print("[test.py] Checking symbol files to upload", flush=True)
    zip_file = zipfile.ZipFile(zip_file_name, "r")

    check_necessary_files_to_upload(zip_file, ["info.txt"])
    check_info_file_to_upload(zip_file)

    symbol_files_count = 0
    for file in zip_file.namelist():
        print(f"[test.py] Found file {file}", flush=True)
        if re.match("libmyapplication_\\w*\\.ysym", file):
            symbol_files_count += 1
    print(f"[test.py] Found {symbol_files_count} symbol files", flush=True)
    if symbol_files_count != 4:
        raise Exception(f"Zip file should contain 4 symbol files, but has {symbol_files_count} with names {zip_file.namelist()}")


def check_mapping_files_to_upload(zip_file_name, mapping_file_name):
    print("[test.py] Checking mapping files to upload", flush=True)
    zip_file = zipfile.ZipFile(zip_file_name, "r")

    check_necessary_files_to_upload(zip_file, ["mapping.txt", "info.txt"])
    check_info_file_to_upload(zip_file)

    mapping_from_zip = zip_file.read("mapping.txt").decode("utf-8")
    with open(mapping_file_name, "r") as f:
        expected_mapping = f.read()
    mappings_are_same = mapping_from_zip == expected_mapping
    print(f"[test.py] Mappings are the same: {mappings_are_same}", flush=True)
    if not mappings_are_same:
        raise Exception("Mapping files should be the same")


def testRelease(temp_sample_path, version_args, wrapper_version, agp_version):
    build_type = "firstOneSecondOneRelease"
    try:
        assemble_and_upload_mappings(
            cwd=temp_sample_path,
            build_type=build_type.capitalize(),
            args=version_args,
        )
        check_mapping_files_to_upload(
            zip_file_name=f"{temp_sample_path}/app/build/appmetrica/{build_type}/result/mapping.zip",
            mapping_file_name=f"{temp_sample_path}/app/build/outputs/mapping/{build_type}/mapping.txt"
        )
        check_symbol_files_to_upload(
            zip_file_name=f"{temp_sample_path}/app/build/appmetrica/{build_type}/result/symbols.zip",
        )
    except Exception as error:
        errors.append(f"wrapper_version={wrapper_version}, agp_version={agp_version}, build_type={build_type}: {str(error)}")


def testDebug(temp_sample_path, version_args, wrapper_version, agp_version):
    testcases = {
        "firstOneSecondOneDebug": False,
        "firstTwoSecondTwoDebug": True,
    }
    for build_type, expect_success in testcases.items():
        try:
            assemble_and_upload_mappings(
                cwd=temp_sample_path,
                build_type=build_type.capitalize(),
                args=version_args,
                expect_success=expect_success,
            )
        except Exception as error:
            errors.append(f"wrapper_version={wrapper_version}, agp_version={agp_version}, build_type={build_type}, expect_success={expect_success}: {str(error)}")


def test():
    publish_plugin_to_maven_local(plugin_path)

    wrapper_version = args.gradle_wrapper_version
    agp_versions = args.agp_version
    sample_path = args.sample_path
    for agp_version in agp_versions:
        temp_sample_path = tempfile.TemporaryDirectory().name
        shutil.copytree(sample_path, temp_sample_path)
        version_args = {
            "agpVersion": agp_version,
            "wrapper.version": wrapper_version,
        }
        try:
            gradle_wrapper(temp_sample_path, version_args)
            show_gradle_version(temp_sample_path)
        except Exception as error:
            errors.append(f"wrapper_version={wrapper_version}, agp_version={agp_version}: {str(error)}")
        testRelease(temp_sample_path, version_args, wrapper_version, agp_version)
        testDebug(temp_sample_path, version_args, wrapper_version, agp_version)

    if len(errors) != 0:
        print(f"Found {str(len(errors))} errors:", file=sys.stderr, flush=True)
        for error in errors:
            print(f"\t{error}", file=sys.stderr, flush=True)
        sys.exit(1)
    else:
        print("All tests passed", flush=True)


if __name__ == "__main__":
    test()
