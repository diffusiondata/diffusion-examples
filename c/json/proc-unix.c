/**
 * Copyright © 2017 Push Technology Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This example is written in C99. Please use an appropriate C99 capable compiler
 *
 * @author Push Technology Limited
 * @since 6.0
 */
/*
 * Execute "ps" on UNIX-like systems (i.e. Linux and OSX), and return a JSON
 * representation of the output.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "list.h"
#include "cJSON.h"

/*
 * Read a space-delimited list of fields for the next process from an open
 * file stream.
 */
static LIST_T *
read_fields(FILE *fp)
{
        char *line = NULL;
        size_t len = 0;
        ssize_t bytes_read = 0;

        if((bytes_read = getline(&line, &len, fp)) == -1) {
                perror("Failed to read fields");
                free(line);
                return NULL;
        }

        // Replace final \n with \0
        if(line[bytes_read - 1] == '\n') {
                line[bytes_read - 1] = '\0';
        }

        LIST_T *fields = list_create();

        char *str_val;
        char *ptr = line;
        while((str_val = strtok(ptr, " ")) != NULL) {
                list_append_last(fields, strdup(str_val));
                ptr = NULL;
        }

        free(line);
        return fields;
}

/*
 * On UNIX-based systems, header items are single words, separated by one or
 * more spaces.
 */
static LIST_T *
read_headers(FILE *fp)
{
        return(read_fields(fp));
}

/*
 * Concatenate two strings with a space separating them. Reallocates memory
 * for the initial string and returns it.
 */
static char *
concat_fields(char *str, const char *val)
{
        size_t orig_size = str == NULL ? 0 : strlen(str);
        size_t add_size = val == NULL ? 0 : strlen(val);

        if(orig_size > 0) {
                // Needs a space between tokens
                str = realloc(str, orig_size + 1 + add_size + 1);
                str[orig_size] = ' ';
                memmove(str + orig_size + 1, val, add_size);
                str[orig_size + 1 + add_size] = '\0';
        }
        else {
                str = malloc(add_size + 1);
                memmove(str, val, add_size);
                str[add_size] = '\0';
        }

        return str;
}

/*
 * Read in processes information from the system and convert to JSON.
 *
 * The JSON will be in the form of an array, where each item is a map of field
 * names to field values; the actual fields depend on the output of the "ps"
 * command used on the system.
 */
cJSON *
read_process_list()
{
#ifdef __APPLE__
        const char *cmd = "ps -x";
#else
        const char *cmd = "ps -u -ww";
#endif

        FILE *fp = popen(cmd, "r");

        cJSON *root = cJSON_CreateObject();
        cJSON *rows = cJSON_CreateArray();

        // The header line of the "ps" output contains the field names (i.e.,
        // the keys for the values).
        LIST_T *headers = read_headers(fp);
        LIST_T *fields;

        // Read in a line of output, and match the values to the field names
        // in the header.
        while((fields = read_fields(fp)) != NULL) {
                LIST_NODE_T *hdr_node = headers->first;
                LIST_NODE_T *fld_node = fields->first;

                cJSON *obj_line = cJSON_CreateObject();
                char *key;
                char *value;
                while(hdr_node != NULL && fld_node != NULL) {

                        key = (char *)hdr_node->data;
                        if(hdr_node->next != NULL) {
                                // If not the last key, the value is a single field.
                                value = strdup((char *)fld_node->data);
                        }
                        else {
                                // For the last key, concatenate all remaining
                                // fields as the value. This is expected to be
                                // the process name & arguments.
                                while(fld_node != NULL) {
                                        value = concat_fields(value, (const char *)fld_node->data);
                                        fld_node = fld_node->next;
                                }
                        }

                        cJSON_AddItemToObject(obj_line, key, cJSON_CreateString(value));

                        // Advance to the next header.
                        hdr_node = hdr_node->next;
                        if(fld_node != NULL) {
                                // Could be NULL if final field
                                fld_node = fld_node->next;
                        }

                        key = NULL;
                        free(value);
                        value = NULL;
                }
                list_free(fields, free);
                cJSON_AddItemToArray(rows, obj_line);
        }

        cJSON_AddItemToObject(root, "processes", rows);

        list_free(headers, free);
        pclose(fp);

        return root;
}
