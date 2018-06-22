"""
    Usage:
    dataDiffCheck.py <old_data_path> <new_data_path>

    Arguments:
        old_data_path: File path of old building json
        new_data_path: File path of new building json
"""
# old.json being the data from https://api.oregonstate.edu/v1/locations?page[size]=10000&type=building
# new.json being the data from https://localhost:8082/api/v0/locations?page[size]=10000&type=building - locations-frontend-api commit b1bec1e013cf29f16a0a01b9dd7c3777e3b4e192

from docopt import docopt
print(docopt(__doc__, version='1.0.0rc2'))

args = docopt(__doc__, version='1.0.0rc2')

import sys
import json

def create_mappings(buildings_json):
    building_data = {}
    names2ids = {}
    #map id to building data
    #map name to id
    for building in buildings_json['data']:
        building_data[building['id']] = building
        names2ids[ building['attributes']['name'] ] = building['id']
        
    return (building_data, names2ids)

if __name__ == "__main__":
    
    if(args['<old_data_path>'] == args['<new_data_path>']):
        print "These are the same file..."
        sys.exit(1)
    else:

        with open(args['<old_data_path>'], "r") as old_json_file:
            with open(args['<new_data_path>'], "r") as new_json_file:
                new_building_data_dict, new_names2ids = create_mappings(json.loads(new_json_file.read()))
                old_building_data_dict, old_names2ids = create_mappings(json.loads(old_json_file.read()))

        old_bdict_view = old_building_data_dict.viewkeys()
        new_bdict_view = new_building_data_dict.viewkeys()

        bkey_intersections = {}
        for k in old_names2ids.viewkeys() & new_names2ids.viewkeys():
            bkey_intersections[k] = {
                "old": old_names2ids[k],
                "new": new_names2ids[k]
            }

        # TODO Add -o output filename option for (OLD,NEW) keyed building json file
        print "\n Outputing buildings with new and old keys to buildingsWithOldNewKeys.json\n"
        with open("buildingsWithOldNewKeys.json", "w") as intersection_file:
            intersection_file.write(json.dumps(
                {"buildings": bkey_intersections},indent=4, sort_keys=True))
        # Report Processing
        rekeyCheckDict = {}
        rekeyedBuildings = []
        totallyNewBuildings = []

        # Dictionary View symetric diff returns set([]) when theres no difference
        if (old_bdict_view ^ new_bdict_view) != set([]):
            # There are differences
            print "\nThese are old ID keys (from the older data) that are no longer present in the new data\n"
            gone_old_buildings = list(old_bdict_view - new_bdict_view)

            for gone_b in gone_old_buildings:
                print gone_b + "  ---  " + old_building_data_dict[gone_b]['attributes']['name']
                rekeyCheckDict[old_building_data_dict[gone_b]
                               ['attributes']['name']] = gone_b

            print "\nThese are new location keys that didn't exist in the old data\n"

            gone_new_buildings = list(new_bdict_view - old_bdict_view)
            for gone_b in gone_new_buildings:
                bname = new_building_data_dict[gone_b]['attributes']['name']
                if rekeyCheckDict.has_key(bname):
                    print gone_b + "  --- REKEYED! ---  " + new_building_data_dict[gone_b]['attributes']['name']
                    rekeyedBuildings.append(gone_b)
                else:
                    totallyNewBuildings.append(gone_b)

            print "\nThese are the totally new buildings\n"
            for gone_b in totallyNewBuildings:
                print gone_b + "  ---  " + new_building_data_dict[gone_b]['attributes']['name']

        else:
            print "There are no differences."

# TODO sort output
