CREATE OR REPLACE FUNCTION GetPersonBasic
  RETURN SYS_REFCURSOR
AS
  p_ResultSet1 SYS_REFCURSOR;
  p_ResultSet2 SYS_REFCURSOR;
BEGIN
    OPEN p_ResultSet1 FOR
        SELECT UUID,NAME,DISPLAYNAME,DESCRIPTION,ICONURL,DETAILEDDESCRIPTION FROM PERSON;
   dbms_sql.return_result(p_ResultSet1);     

    OPEN p_ResultSet2 FOR
        SELECT UUID,NAME,DISPLAYNAME,DESCRIPTION,ICONURL,DETAILEDDESCRIPTION FROM PERSON;
   dbms_sql.return_result(p_ResultSet2);             
END GetPersonBasic;