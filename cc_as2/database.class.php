<?php

class DataStore
    {
    var $Host     = "localhost";        // Hostname of our MySQL server.
    var $Database = "youtube";     // Logical database name on that server.
    var $User     = "root";             // User and Password for login.
    var $Password = "edward.890531";
 
    var $Link_ID  = 0;                  // Result of mysql_connect().
    var $Query_ID = 0;                  // Result of most recent mysql_query().
    var $Record   = array();            // current mysql_fetch_array()-result.
    var $Row;                           // current row number.
    var $LoginError = "";
 
    var $Errno    = 0;                  // error state of query...
    var $Error    = "";
    
    private static $db;
    
    public static function getInstance(){
        if(self::$db == null){
            self::$db = new Database();
            return self::$db;
        }
        return self::$db;
    }
    

   
//Setup connection to datastore
    function connect()
        {
        if( 0 == $this->Link_ID )
            $this->Link_ID=mysql_connect( $this->Host, $this->User, $this->Password );
        if( !$this->Link_ID )
            $this->halt( "Link-ID == false, connect failed" );
        if( !mysql_query( sprintf( "use %s", $this->Database ), $this->Link_ID ) )
            $this->halt( "cannot use database ".$this->Database );
        } // end function connect
 
//Setup query to datastore
    function query( $Query_String )
        {
        $this->connect();
        $this->Query_ID = mysql_query( $Query_String,$this->Link_ID );
        $this->Row = 0;
        $this->Errno = mysql_errno();
        $this->Error = mysql_error();
        if( !$this->Query_ID )
            $this->halt( "Invalid SQL: ".$Query_String );
        return $this->Query_ID;
        } // end function query

//Setup the query array to datastore
    function queryArray($Query_String){
        $result = array();
        $this->query($Query_String);
        while($this->nextRecord()){
                array_push($result, $this->Record);
            }
        return $result;
    	}

//Read single record
	function singleRecord(){
	    $this->Record = mysql_fetch_array( $this->Query_ID );
	    $stat = is_array( $this->Record );
	    return $stat;
	    } // end function singleRecord
 

//Read next record
    function nextRecord()
        {
        @ $this->Record = mysql_fetch_array( $this->Query_ID );
        $this->Row += 1;
        $this->Errno = mysql_errno();
        $this->Error = mysql_error();
        $stat = is_array( $this->Record );
        if( !$stat )
            {
            @ mysql_free_result( $this->Query_ID );
            $this->Query_ID = 0;
            }
        return $stat;
        } // end function nextRecord
 

//Read number of rows
    function numRows()
        {
        return mysql_num_rows( $this->Query_ID );
        } // end function numRows
		
//Read number of affected rows
	function affectedRows()
		{
		return mysql_affected_rows();
		} // end function numRows

//Read last insert id
    function lastId()
        {
        return mysql_insert_id();
        } // end function numRows
 
//Read escaped string
     function mysql_escape_mimic($inp)
        {
        if(is_array($inp))
            return array_map(__METHOD__, $inp);
        if(!empty($inp) && is_string($inp)) {
            return str_replace(array('\\', "\0", "\n", "\r", "'", '"', "\x1a"), array('\\\\', '\\0', '\\n', '\\r', "\\'", '\\"', '\\Z'), $inp);
        }
        return $inp;
        }
		

 
//Read number of fields in a recordset
     function numFields()
        {
            return mysql_num_fields($this->Query_ID);
        } // end function numRows

//Error response
     function halt( $msg )
	    {
		    printf( "
			<strong>Error:</strong> %s
			n", $msg );
		    	printf( "<strong>MySQL Error</strong>: %s (%s)
				n", $this->Errno, $this->Error );
		        die( "Program halted." );
		} // end function halt
 

 
    } // end class Database

?>
