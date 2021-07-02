#!/usr/bin/env python3

# This file is testing, if the structure of the different .json localization files
# matches the structure of the english file. It also checks for matching string placeholders.

# Author: Patschke (github.com/Patschke)

ROOT_DIR = "../"
LOCALIZATION_PATH = "src/main/resources/chrono/localization/"
DEFAULT_LANG = "eng"

import json
import logging
import os

def main():
    """The main method. this will run the main loops"""

    logging.getLogger().setLevel(logging.INFO)

    basepath = os.path.join(ROOT_DIR, LOCALIZATION_PATH)
    logging.info(f"Checking validity of json files in {basepath}")

    # get the default folder which we want to compare everything against
    defaultpath = os.path.join(basepath, DEFAULT_LANG)

    # now we iterate file by file (e.g. blights.json and ui.json)
    for file in os.listdir(defaultpath):
        logging.info(f"Checking {file}:")
        # read default file
        with open(os.path.join(defaultpath, file)) as f:
            defaultJSON = json.load(f)
        # iterate over languages
        for lang in os.listdir(basepath):
            langpath = os.path.join(basepath, lang)
            if not os.path.isdir(langpath):
                continue
            logging.info(f"Checking {lang}")
            # read language file
            with open(os.path.join(langpath, file), encoding='utf-8') as f:
                langJSON = json.load(f)
            if compare(defaultJSON, langJSON, position=f"{file}->{lang}"):
                logging.info(f"No errors found")


def compare(a, b, position=""):
    """Compares two things a, b to match. If a, b are dicts, all keys of a 
    have to be in b, and the values either to match recursively. If a, b are
    strings they match if they have equal numer of placeholders in them, 
    if they are lists their elements have to match. Position provides 
    information where in the json we currently are"""
    
    # check if types match
    if not type(a) is type(b):
        logging.error(f"Value for {position} is of wrong type ({type(b)} instead of {type(a)})")
        return False

    # recursive compare two lists
    if isinstance(a, list):
        # check if length matches
        if len(a) != len(b):
            logging.error(f"Lists at {position} has wrong length ({len(b)} instead of {len(a)})")
            return False
        out = True
        # recursive check if entries match
        for i, (entrya, entryb) in enumerate(zip(a, b)):
            out = out and compare(entrya, entryb, position+f"->ListElement[{i}]")
        return out

    # compare two strings
    elif isinstance(a, str):
        # split at % signs, ignore first part
        for parta, partb in zip(a.split('%')[1:], b.split('%')[1:]):
            # check if first letter after % is identical
            if parta[0] != partb[0]:
                logging.error(f"Strings at {position} have different placeholder (or use % text. Ignore this error in that case)")
                return False
        return True


    # recursive compare two dicts
    elif isinstance(a, dict):
        out = True
        for key in a:
            # check if key exists
            if not key in b:
                logging.error(f"Key {key} is missing in {position}")
                out = False 
                continue

            # compare recursively
            out = out and compare(a[key], b[key], position+f"->{key}")
        return out

    # not sure what else we could encounter. Just test for equality?
    else:
        if vala != valb:
            logging.error(f"Some differenc I don't understand in {position}: {vala} != {valb}")
            return False
        return True



if __name__=='__main__':
    main()
