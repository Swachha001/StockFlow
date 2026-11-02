-- ################################################################################################
-- #  DANGER — THIS SCRIPT PERMANENTLY DELETES stockflow_db AND EVERYTHING IN IT.                 #
-- #  It does nothing unless you first change  'NO'  to  'YES'  on the SET line below.            #
-- #  Afterwards run 01_schema.sql and 02_seed_data.sql again.                                    #
-- ################################################################################################
SET @I_UNDERSTAND_THIS_DELETES_ALL_STOCKFLOW_DATA = 'YES';

SET @sf_sql = IF(@I_UNDERSTAND_THIS_DELETES_ALL_STOCKFLOW_DATA = 'YES',
                 'DROP DATABASE IF EXISTS stockflow_db',
                 'SELECT ''Nothing was deleted. Change NO to YES at the top of this file to really reset.'' AS result');
PREPARE sf_stmt FROM @sf_sql;
EXECUTE sf_stmt;
DEALLOCATE PREPARE sf_stmt;
