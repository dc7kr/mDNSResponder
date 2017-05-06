/*
 * Copyright (c) 2004 Apple Computer, Inc. All rights reserved.
 *
 * @APPLE_LICENSE_HEADER_START@
 * 
 * Copyright (c) 1999-2003 Apple Computer, Inc.  All Rights Reserved.
 * 
 * This file contains Original Code and/or Modifications of Original Code
 * as defined in and that are subject to the Apple Public Source License
 * Version 2.0 (the 'License'). You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at
 * http://www.opensource.apple.com/apsl/ and read it before using this
 * file.
 * 
 * The Original Code and all software distributed under the License are
 * distributed on an 'AS IS' basis, WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, AND APPLE HEREBY DISCLAIMS ALL SUCH WARRANTIES,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT.
 * Please see the License for the specific language governing rights and
 * limitations under the License.
 * 
 * @APPLE_LICENSE_HEADER_END@

    Change History (most recent first):

$Log: DNSSDUnitTest.java,v $
Revision 1.3  2004/05/26 01:41:58  cheshire
Pass proper flags to DNSSD.enumerateDomains

Revision 1.2  2004/04/30 21:53:34  rpantos
Change line endings for CVS.

Revision 1.1  2004/04/30 16:29:35  rpantos
First checked in.

	DNSSDUnitTest is a simple program that exercises parts of the DNSSD API.
 */

import com.apple.dnssd.*;

import java.net.*;
import java.util.*;


class	DNSSDUnitTest
{
	public static final String	TEST_TYPE = "_unittest._udp";
	public static final String	WIRE_CHAR_SET = "ISO-8859-1";

	public DNSSDUnitTest	fInstance = null;

	public		DNSSDUnitTest() throws Exception
	{
		fStage = 0;
		fInstance = this;
		
		Enumeration	en = NetworkInterface.getNetworkInterfaces();
		while ( en.hasMoreElements())
			System.out.println( ((NetworkInterface) en.nextElement()).getName());
	}

	public void	testTxtRecord()
	{
		byte[]		src = { 6, 'a', 't', '=', 'X', 'Y', 'Z' };
		TXTRecord	txtRecord = new TXTRecord( src);
		String		a;
		
		txtRecord.set( "path", "~/names");
		txtRecord.set( "ttl", "4");

		byte[]	rawBytes = txtRecord.getRawBytes();
		System.out.println( ( new String( rawBytes, 0, rawBytes.length)) + " has count " + 
								String.valueOf( txtRecord.size()));

		boolean		ttlPresent = txtRecord.contains( "ttl");
		System.out.println( "ttl is present: " + ( ttlPresent ? "true" : "false"));
		boolean		timeoutPresent = txtRecord.contains( "timeout");
		System.out.println( "timeout is present: " + ( timeoutPresent ? "true" : "false"));

		for ( int i=0; null != ( a = txtRecord.getKey( i)); i++)
			System.out.println( "attr/val " + String.valueOf( i) + ": " + a + "," + txtRecord.getValueAsString( i));
	}

	public void	run() throws DNSSDException
	{
		System.out.println( "Running DNSSD unit test for " + System.getProperty( "user.name"));

		this.testTxtRecord();

		fRegTest = new RegTest();
		new BrowseTest();
		new DomainTest();
		
		this.waitForEnd();
	}

	protected int		fStage;
	protected RegTest	fRegTest;

	public synchronized void bumpStage()
	{
		fStage++;
		this.notifyAll();
	}

	protected synchronized void waitForEnd()
	{
		int stage = fStage;
		while ( stage == fStage)
		{
			try {
				wait();
			} catch (InterruptedException e) {}
		}
	}

    public static void main(String s[]) 
    {
    	try {
			new DNSSDUnitTest().run();
		}
		catch ( Exception e) { terminateWithException( e); }
    }

	protected static void	terminateWithException( Exception e)
	{
		e.printStackTrace();
		System.exit( -1);
	}
}

class	TermReporter implements BaseListener
{
	public void	operationFailed( DNSSDService service, int errorCode)
	{
		System.out.println( this.getClass().getName() + " encountered error " + String.valueOf( errorCode));
	}

	protected void finalize() throws Throwable
	{
		System.out.println( "Instance of " + this.getClass().getName() + " has been destroyed");
	}
}

class	RegTest extends TermReporter implements RegisterListener
{
	public static final int		TEST_PORT = 5678;

	public		RegTest() throws DNSSDException
	{
		fReg = DNSSD.register( 0, 0, "Test service", DNSSDUnitTest.TEST_TYPE, "", "", TEST_PORT, null, this);
	}

	public void	serviceRegistered( DNSSDRegistration registration, int flags, String serviceName, 
								String regType, String domain)
	{
		String s = "RegTest result flags:" + String.valueOf( flags) + 
					" serviceName:" + serviceName + " regType:" + regType + " domain:" + domain;
		System.out.println( s);

		try {
			new DupRegTest();
			
			byte[]	kResponsiblePerson = { 'c','o','o','k','i','e',' ','m','o','n','s','t','e','r' };
			fReg.addRecord( 0, 17 /*ns_t_rp*/, kResponsiblePerson, 3600);
			new QueryTest( 0, 0, "Test service", 17 /*ns_t_rp*/, 1);
		} catch( Exception e) { e.printStackTrace(); }
	}

	protected DNSSDRegistration	fReg;
}

class	DupRegTest extends TermReporter implements RegisterListener
{
	public static final int		TEST_PORT = 5678;

	public		DupRegTest() throws DNSSDException
	{
		DNSSD.register( DNSSD.NO_AUTO_RENAME | DNSSD.UNIQUE, 0, "Test service", DNSSDUnitTest.TEST_TYPE, "", "", TEST_PORT + 1, null, this);
	}

	public void	serviceRegistered( DNSSDRegistration registration, int flags, String serviceName, 
								String regType, String domain)
	{
		System.out.println( "Oik - registered a duplicate!");
		String s = "DupRegTest result flags:" + String.valueOf( flags) + 
					" serviceName:" + serviceName + " regType:" + regType + " domain:" + domain;
		System.out.println( s);
	}
}

class	BrowseTest extends TermReporter implements BrowseListener
{
	public		BrowseTest()
	{
		try {
			DNSSD.browse( 0, 0, DNSSDUnitTest.TEST_TYPE, "", this);
		} catch( Exception e) { e.printStackTrace(); }
	}

	public void	serviceFound( DNSSDService browser, int flags, int ifIndex, 
							String serviceName, String regType, String domain)
	{
		String s = "BrowseTest found flags:" + String.valueOf( flags) + 
					" ifIndex:" + String.valueOf( ifIndex) + 
					" serviceName:" + serviceName + " regType:" + regType + " domain:" + domain;
		System.out.println( s);
		
		System.out.println( "Resolving " + serviceName);
		new ResolveTest( 0, ifIndex, serviceName, regType, domain);
	}

	public void	serviceLost( DNSSDService browser, int flags, int ifIndex,
							String serviceName, String regType, String domain)
	{
		String s = "BrowseTest lost flags:" + String.valueOf( flags) + 
					" ifIndex:" + String.valueOf( ifIndex) + 
					" serviceName:" + serviceName + " regType:" + regType + " domain:" + domain;
		System.out.println( s);
	}

	public void	operationFailed( DNSSDService service, int errorCode)
	{
		System.out.println( "Browse failed " + String.valueOf( errorCode));
	}
}

class	DomainTest extends TermReporter implements DomainListener
{
	public		DomainTest()
	{
		try {
			DNSSD.enumerateDomains( DNSSD.BROWSE_DOMAINS, 0, this);
		} catch( Exception e) { e.printStackTrace(); }
	}

	public void	domainFound( DNSSDService enumerator, int flags, int ifIndex, String domain)
	{
		String s = "Domain found flags:" + String.valueOf( flags) + 
					" ifIndex:" + String.valueOf( ifIndex) + 
					" domain:" + domain;
		System.out.println( s);
	}

	public void	domainLost( DNSSDService enumerator, int flags, int ifIndex, String domain)
	{
		String s = "Domain lost flags:" + String.valueOf( flags) + 
					" ifIndex:" + String.valueOf( ifIndex) + 
					" domain:" + domain;
		System.out.println( s);
	}

	public void	operationFailed( DNSSDService service, int errorCode)
	{
		System.out.println( "Domain enum op failed " + String.valueOf( errorCode));
	}
}

class	ResolveTest extends TermReporter implements ResolveListener
{
	public		ResolveTest( int flags, int ifIndex, String serviceName, String regType, 
							String domain)
	{
		try {
			DNSSD.resolve( flags, ifIndex, serviceName, regType, domain, this);
		} catch( Exception e) { e.printStackTrace(); }
	}

	public void	serviceResolved( DNSSDService resolver, int flags, int ifIndex, String fullName, 
								String hostName, int port, TXTRecord txtRecord)
	{
		String a;
		String s = "ResolveTest result flags:" + String.valueOf( flags) + 
					" ifIndex:" + String.valueOf( ifIndex) + 
					" fullName:" + fullName + " hostName:" + hostName + " port:" + String.valueOf( port);
		for ( int i=0; null != ( a = txtRecord.getKey( i)); i++)
			s += " attr/val " + String.valueOf( i) + ": " + a + "," + txtRecord.getValueAsString( i);

		System.out.println( s);

		System.out.println( "Querying " + hostName);
		new QueryTest( 0, ifIndex, hostName, 1 /* ns_t_a */, 1 /* ns_c_in */);
	}
}

class	QueryTest extends TermReporter implements QueryListener
{
	public		QueryTest( int flags, int ifIndex, String serviceName, int rrtype, int rrclass)
	{
		try {
			DNSSD.queryRecord( flags, ifIndex, serviceName, rrtype, rrclass, this);
		} catch( Exception e) { e.printStackTrace(); }
	}

	public void	queryAnswered( DNSSDService query, int flags, int ifIndex, String fullName, 
								int rrtype, int rrclass, byte[] rdata, int ttl)
	{
		String s = "QueryTest result flags:" + String.valueOf( flags) + 
					" ifIndex:" + String.valueOf( ifIndex) + 
					" fullName:" + fullName + " rrtype:" + String.valueOf( rrtype) + 
					" rrclass:" + String.valueOf( rrclass) + " ttl:" + String.valueOf( ttl);
		System.out.println( s);

		try {
			String	dataTxt = new String( rdata, 0, rdata.length, DNSSDUnitTest.WIRE_CHAR_SET);
			System.out.println( "Query data is:" + dataTxt);
		} catch( Exception e) { e.printStackTrace(); }
	}
}
