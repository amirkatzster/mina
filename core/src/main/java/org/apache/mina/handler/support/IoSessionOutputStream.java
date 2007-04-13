/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.mina.handler.support;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;

/**
 * An {@link OutputStream} that forwards all write operations to
 * the associated {@link IoSession}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 *
 */
public class IoSessionOutputStream extends OutputStream
{
    private final IoSession session;
    private WriteFuture lastWriteFuture;
    
    public IoSessionOutputStream( IoSession session )
    {
        this.session = session;
    }
    
    public void close() throws IOException
    {
        try {
            flush();
        } finally {
            session.close().join();
        }
    }

    private void checkClosed() throws IOException
    {
        if( ! session.isConnected() )
        {
            throw new IOException( "The session has been closed." );
        }
    }
    
    private synchronized void write( ByteBuffer buf ) throws IOException
    {
        checkClosed();
        WriteFuture future = session.write( buf );
        lastWriteFuture = future;
    }
    
    public void write( byte[] b, int off, int len ) throws IOException
    {
        write( ByteBuffer.wrap( (byte[]) b.clone(), off, len ) );
    }

    public void write( int b ) throws IOException
    {
        ByteBuffer buf = ByteBuffer.allocate( 1 );
        buf.put( ( byte ) b );
        buf.flip();
        write( buf );
    }
    
    public synchronized void flush() throws IOException
    {
        if( lastWriteFuture == null )
        {
            return;
        }
        
        lastWriteFuture.join();
        if( !lastWriteFuture.isWritten() )
        {
            throw new IOException( "The bytes could not be written to the session" );
        }
    }
}
