/*
 * Copyright 2023-2025, Seqera Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.seqera.wave.cli.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
public class StreamHelper {

    private static final Logger log = LoggerFactory.getLogger(StreamHelper.class);

    static public String tryReadStdin() {
        return tryReadStream(System.in);
    }

    static public String tryReadStream(InputStream stream) {
        try {
            if( stream.available()==0 )
                return null;
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while( (len=stream.read(buffer))!=-1 ) {
                result.write(buffer,0,len);
            }
            return new String(result.toByteArray());
        }
        catch (IOException e){
            log.debug("Unable to read system.in", e);
        }
        return null;
    }

}
