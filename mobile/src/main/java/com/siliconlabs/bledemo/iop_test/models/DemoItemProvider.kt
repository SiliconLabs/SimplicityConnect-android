package com.siliconlabs.bledemo.iop_test.models


class DemoItemProvider {
    companion object {
        private fun getDataTest(): ArrayList<ItemTestCaseInfo> {
            return ArrayList<ItemTestCaseInfo>().apply {
                add(ItemTestCaseInfo(1, "Scan device", "Central starts scanning and looking for " + "\"IOP Test\" device.", null, Common.IOP3_TC_STATUS_WAITING))
                add(ItemTestCaseInfo(2, "Connect to device", "Central connects to peripheral.", null, Common.IOP3_TC_STATUS_WAITING))
                add(ItemTestCaseInfo(3, "Central discovers the GATT", "Central discovers the GATT database from peripheral.", null, Common.IOP3_TC_STATUS_WAITING))
                add(ItemTestCaseInfo(4, "Central performs all GATT", "Central performs all GATT operations supported by target.", dataChildrenTest(), Common.IOP3_TC_STATUS_WAITING))
                add(ItemTestCaseInfo(5, "IOP Test OTA update with ACK", "Update user application via OTA with ACK.", null, Common.IOP3_TC_STATUS_WAITING))
                add(ItemTestCaseInfo(6, "IOP Test OTA update without ACK", "Update user application via OTA without ACK.", null, Common.IOP3_TC_STATUS_WAITING))
                add(ItemTestCaseInfo(7, "Throughput", "Throughput-GATT Notification.", null, Common.IOP3_TC_STATUS_WAITING))
                add(ItemTestCaseInfo(8, "Security and Encryption", "Security and Encryption.", dataChildrenSecurity(), Common.IOP3_TC_STATUS_WAITING))
                
                /* This test should remain hidden for now due to investigation being underway. It may return later.
                add(ItemTestCaseInfo(9, "GATT Caching", "GATT Caching.", dataChildrenCaching(), Common.IOP3_TC_STATUS_WAITING))
                 */
            }
        }

        fun createDataSiliconLabsTest(fwName: String): SiliconLabsTestInfo {
            return SiliconLabsTestInfo(fwName, getDataTest())
        }

        private fun dataChildrenTest(): ArrayList<ChildrenItemTestInfo> {
            return ArrayList<ChildrenItemTestInfo>().apply {
                add(ChildrenItemTestInfo(1, "", "IOP Test Read Only Length 1", "Read"))
                add(ChildrenItemTestInfo(2, "", "IOP Test Read Only Length 255", "Read"))
                add(ChildrenItemTestInfo(3, "", "IOP Test Write Only Length 1", "Write"))
                add(ChildrenItemTestInfo(4, "", "IOP Test Write Only length 255", "Write"))
                add(ChildrenItemTestInfo(5, "", "IOP Test Write Without Response Length 1", " Write Without Response"))
                add(ChildrenItemTestInfo(6, "", "IOP Test Write Without Response Length 255", " Write Without Response"))
                add(ChildrenItemTestInfo(7, "", "IOP Test Notify length 1", "Notify"))
                add(ChildrenItemTestInfo(8, "", "IOP Test Notify length MTU - 3", "Notify"))
                add(ChildrenItemTestInfo(9, "", "IOP Test Indicate Length 1", "Indicate"))
                add(ChildrenItemTestInfo(10, "", "IOP Test Indicate length MTU - 3", "Indicate"))
                add(ChildrenItemTestInfo(11, "Characteristic", "IOP Test Length 1", "Read,Write"))
                add(ChildrenItemTestInfo(12, "Characteristic", "IOP Test Length 255", "Read,Write"))
                add(ChildrenItemTestInfo(13, "Characteristic", "IOP Test Length Variable 4", "Read,Write"))
                add(ChildrenItemTestInfo(14, "Characteristic", "IOP Test Const Length 1", "Read,Write"))
                add(ChildrenItemTestInfo(15, "Characteristic", "IOP Test Const Length 255", "Read,Write"))
                add(ChildrenItemTestInfo(16, "Characteristic", "IOP Test User Len 1", "Read,Write"))
                add(ChildrenItemTestInfo(17, "Characteristic", "IOP Test User Len 255", "Read,Write"))
                add(ChildrenItemTestInfo(18, "Characteristic", "IOP Test User Len Variable 4", "Read,Write"))
            }
        }

        fun dataChildrenOTA(): ArrayList<ChildrenItemTestInfo> {
            return ArrayList<ChildrenItemTestInfo>().apply {
                add(ChildrenItemTestInfo(1, "", "OTA update-Acknowledged write", "Write"))
                add(ChildrenItemTestInfo(2, "", "OTA update-Unacknowledged write", "Write"))
            }
        }

/* These test cases should remain hidden for now due to investigation being underway. They may return later.

        private fun dataChildrenCaching(): ArrayList<ChildrenItemTestInfo> {
            return ArrayList<ChildrenItemTestInfo>().apply {
                add(ChildrenItemTestInfo(1, "", "IOP Test GATT Caching runtime", ""))
                add(ChildrenItemTestInfo(2, "", "IOP Test Service change indications", ""))
            }
        }
*/

        private fun dataChildrenSecurity(): ArrayList<ChildrenItemTestInfo> {
            return ArrayList<ChildrenItemTestInfo>().apply {
                add(ChildrenItemTestInfo(1, "", "IOP Test Security-Pairing", ""))
                add(ChildrenItemTestInfo(2, "", "IOP Test Security-Authentication", ""))
                add(ChildrenItemTestInfo(3, "", "IOP Test Security-Bonding", ""))
            }
        }
    }
}