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
 * Execute "tasklist" on Windows systems, and return a JSON representation of
 * the output.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

#include "list.h"
#include "cJSON.h"

/*
 * Struct that records the name of a field in the process output, and the
 * maximum length of that field.
 */
typedef struct {
        int width;
        char *name;
} HEADER_T;

static void
free_header(HEADER_T *header)
{
        if(header == NULL) {
                return;
        }
        free(header->name);
        free(header);
}

/*
 * Returns a copy of the input string with leading and trailing whitespace
 * removed.
 */
static char *
trim_whitespace(const char *str)
{
        char *end = (char *)str + strlen(str) - 1;

        while(end > str && isspace(*end)) {
                end--;
        }

        char *start = (char *)str;
        while(str < end && isspace(*start)) {
                start++;
        }

        size_t len = (end + 1) - start;
        char *trimmed = malloc(len + 1);
        memmove(trimmed, start, len);
        trimmed[len] = '\0';
        return trimmed;
}

/*
 * tasklist.exe outputs headers for process information over two lines. The
 * first line contains the names of the fields (which may be multiple words
 * per field), and the second line contains separators consisting of "="
 * characters and a single space between each field, e.g.:
 *
 * Process Name              PID Session Name
 * ==================== ======== =================
 *
 * We look at the length of each separator, which correlates with the length
 * of the field name and allows us to extract it reliably. We also need this
 * length for later parsing of each field in the process output.
 */
static LIST_T *
read_headers(FILE *fp)
{
        // Read the line containing the headers (field names).
        char header_line[1024];
        if(fgets(header_line, 1024, fp) == NULL) {
                perror(NULL);
                return NULL;
        }

        // Read the line containing the separators.
        char sep_line[1024];
        if(fgets(sep_line, 1024, fp) == NULL) {
                perror(NULL);
                return(NULL);
        }

        LIST_T *headers = list_create();

        // Split the separator line at each space, which lets us calculate the
        // length of each field and subsequently, the name.
        char *ptr = sep_line;
        char *sep;
        while((sep = strtok(ptr, " ")) != NULL) {
                HEADER_T *header = malloc(sizeof(HEADER_T));
                header->width = strlen(sep);

                size_t offset = (sep - sep_line);
                *(header_line + offset + header->width) = '\0';

                header->name = trim_whitespace(header_line + offset);

                list_append_last(headers, header);
                ptr = NULL;
        }

        return headers;
}

/*
 * With knowledge of the maximum length of each field from read_headers(), we
 * can read the next line of process information.
 */
static LIST_T *
read_fields(FILE *fp, LIST_T *headers)
{
        // Arbitraty number to simplify code slightly; in reality there's unlikely to be more than 5
        const int MAX_HEADERS = 100;

        // Create array of field lengths
        int header_size[100];
        LIST_NODE_T *node = (headers != NULL ? headers->first : NULL);
        int header_count = 0;
        while(node != NULL && header_count < MAX_HEADERS) {
                header_size[header_count++] = ((HEADER_T *)node->data)->width;
                node = node->next;
        }

        LIST_T *fields = NULL;

        char line[1024];
        if(fgets(line, 1024, fp) == NULL) {
                return NULL;
        }

        fields = list_create();

        size_t offset = 0;
        for(int i = 0; i < header_count; i++) {
                char *tmp_str = malloc(header_size[i] + 1);
                memcpy(tmp_str, line + offset, header_size[i]);
                tmp_str[header_size[i]] = '\0';
                char *trimmed = trim_whitespace(tmp_str);
                list_append_last(fields, trimmed);
                offset += header_size[i] + 1;
                free(tmp_str);
        }

        return fields;
}

/*
 * Read in process information from the system and convert to JSON.
 *
 * The JSON will be in the form of an array, where each item is a map of field
 * names to field values; the actual fields depend on the output of the "ps"
 * command used on the system.
 */
cJSON *
read_process_list()
{
        const char *cmd = "tasklist.exe /v";

        FILE *fp = _popen(cmd, "r");

        cJSON *root = cJSON_CreateObject();

        cJSON *rows = cJSON_CreateArray();

        // The header line of the "tasklist.exe" output contains the field
        // names (i.e., the keys for the values).
        LIST_T *headers = read_headers(fp);
        LIST_T *fields;

        // Read in a line of output, and match the values to the field names
        // in the header.
        while((fields = read_fields(fp, headers)) != NULL) {
                LIST_NODE_T *hdr_node = headers->first;
                LIST_NODE_T *fld_node = fields->first;

                cJSON *obj_line = cJSON_CreateObject();
                char *key;
                char *value;
                while(hdr_node != NULL && fld_node != NULL) {

                        key = ((HEADER_T *)hdr_node->data)->name;
                        if(hdr_node->next == NULL) {
                                value = strdup((char *)fld_node->data);
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

        list_free(headers, (void (*)(void *))free_header);
        _pclose(fp);

        return root;
}
