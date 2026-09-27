import importlib.util
import unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location('migration',Path(__file__).with_name('migrate-push-neon.py'))
migration=importlib.util.module_from_spec(spec)
spec.loader.exec_module(migration)
class MigrationTest(unittest.TestCase):
    def test_preserves_identity_audit_columns_and_unknown_history(self):
        row=migration.convert_row('tb_push_dispatch',{'pushDispatchId':4,'createdAt':'time'},['push_dispatch_id','created_at','admin_only','audience'])
        self.assertEqual(row,{'push_dispatch_id':4,'created_at':'time','admin_only':None,'audience':'LEGACY_UNKNOWN'})
    def test_pending_schedules_require_verified_classification(self):
        with self.assertRaises(ValueError):
            migration.convert_row('tb_survey_push_schedule',{'status':'PENDING'},['status','audience'])
        row=migration.convert_row('tb_survey_push_schedule',{'status':'PENDING'},['status','audience'],'LIVE')
        self.assertEqual(row['audience'],'LIVE')
    def test_mysql_bit_zero_does_not_activate_a_device(self):
        for raw, expected in [(b'\x00',False),(b'\x01',True),(0,False),(1,True)]:
            self.assertIs(migration.convert_row('tb_push_installation',{'active':raw},['active'])['active'],expected)
    def test_existing_receipts_and_status_are_never_requeued(self):
        row={'status':'DELIVERED','expo_ticket_id':'ticket','send_attempts':2}
        self.assertEqual(migration.convert_row('tb_push_message',row,list(row)),row)
if __name__=='__main__': unittest.main()
