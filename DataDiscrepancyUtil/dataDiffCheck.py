import sys
import json

if __name__ == "__main__":
    print("Expecting old.json and new.json as input files 1 and 2.")
    #old.json being the data from https://api.oregonstate.edu/v1/locations?page[size]=10000&type=building
    #new.json being the data from https://localhost:8082/api/v0/locations?page[size]=10000&type=building - locations-frontend-api commit b1bec1e013cf29f16a0a01b9dd7c3777e3b4e192

    if(len(sys.argv) >= 3):
        if(sys.argv[1] == sys.argv[2]):
            print "These are the same file..."
        print sys.argv[1] + " as old_file"
        print sys.argv[2] + " as new_file"

        with open(sys.argv[1],"r") as old_file:
            old_building_data_json = json.loads(old_file.read())
        with open(sys.argv[2],"r") as new_file:
            new_building_data_json = json.loads(new_file.read())
            
        #new_building_data_json = #open json file with the new data or setup logic to make api request. https://localhost:8080/v0/locations?page[size]=4500&type=building
        #old_building_data_json = #open json file with hold data or setup logic to make api request. https://oregonstateuniversity-dev.apigee.net/v1/locations?page[size]=4500&type=building

        new_building_data_dict = {}
        old_building_data_dict = {}

        #Create a dict of the new building data to make the next loop simpler
        for new_building in new_building_data_json['data']:
            new_building_data_dict[new_building['id']] = new_building

        for old_building in old_building_data_json['data']:
            old_building_data_dict[old_building['id']] = old_building

        old_bdict_view = old_building_data_dict.viewkeys()
        new_bdict_view = new_building_data_dict.viewkeys()
        
        rekeyCheckDict = {}
        rekeyedBuildings = []
        totallyNewBuildings = []

        if (old_bdict_view ^ new_bdict_view) != set([]): #Dictionary View symetric diff returns set([]) when theres no difference
            #There are differences
            print "\nThese are the keys from the older data that is no longer present in the new data\n"
            gone_old_buildings = list(old_bdict_view - new_bdict_view)
            for gone_b in gone_old_buildings:
                print gone_b + "  ---  " + old_building_data_dict[gone_b]['attributes']['name']
                rekeyCheckDict[ old_building_data_dict[gone_b]['attributes']['name'] ] = gone_b
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
            #print new_bdict_view - old_bdict_view
        else:
            print "There are no differences."