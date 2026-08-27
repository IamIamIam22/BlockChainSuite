package com.example.data.repository

import com.example.data.local.VirtualMachineDao
import com.example.data.local.VirtualMachineEntity
import com.example.data.model.VirtualMachine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VirtualMachineRepository(private val vmDao: VirtualMachineDao) {

    val allVms: Flow<List<VirtualMachine>> = vmDao.getAllVms().map { entities ->
        if (entities.isEmpty()) {
            emptyList()
        } else {
            entities.map { it.toModel() }
        }
    }

    suspend fun initializeDefaultVmsIfEmpty() {
        // Create initial default VMs if none exist
        val default1 = VirtualMachineEntity(
            id = "vm-ubuntu-01",
            name = "Ubuntu 24.04 LTS Node",
            distro = "Ubuntu 24.04 LTS (Noble)",
            region = "us-east-1 (N. Virginia)",
            ipAddress = "10.240.12.89",
            publicIp = "34.120.45.198",
            isRunning = true,
            cpuCores = 4,
            ramMb = 8192,
            diskGb = 80
        )
        val default2 = VirtualMachineEntity(
            id = "vm-alpine-02",
            name = "Alpine Micro Cloud",
            distro = "Alpine Linux 3.20",
            region = "eu-central-1 (Frankfurt)",
            ipAddress = "10.180.4.12",
            publicIp = "35.242.110.45",
            isRunning = false,
            cpuCores = 2,
            ramMb = 4096,
            diskGb = 40
        )
        val default3 = VirtualMachineEntity(
            id = "vm-arch-03",
            name = "Arch Linux Edge VM",
            distro = "Arch Linux Rolling",
            region = "ap-northeast-1 (Tokyo)",
            ipAddress = "10.90.8.21",
            publicIp = "34.85.201.78",
            isRunning = false,
            cpuCores = 8,
            ramMb = 16384,
            diskGb = 160
        )

        vmDao.insertVm(default1)
        vmDao.insertVm(default2)
        vmDao.insertVm(default3)
    }

    suspend fun createVm(
        name: String,
        distro: String,
        region: String,
        cpuCores: Int,
        ramMb: Int,
        diskGb: Int
    ): VirtualMachine {
        val id = "vm-${System.currentTimeMillis() % 100000}"
        val randIp = "10.${(100..250).random()}.${(1..250).random()}.${(1..250).random()}"
        val randPub = "34.${(100..250).random()}.${(1..250).random()}.${(1..250).random()}"

        val entity = VirtualMachineEntity(
            id = id,
            name = name,
            distro = distro,
            region = region,
            ipAddress = randIp,
            publicIp = randPub,
            isRunning = true,
            cpuCores = cpuCores,
            ramMb = ramMb,
            diskGb = diskGb
        )
        vmDao.insertVm(entity)
        return entity.toModel()
    }

    suspend fun toggleVmPower(vm: VirtualMachine) {
        val updated = vm.copy(isRunning = !vm.isRunning)
        vmDao.updateVm(updated.toEntity())
    }

    suspend fun deleteVm(id: String) {
        vmDao.deleteVm(id)
    }

    private fun VirtualMachineEntity.toModel(): VirtualMachine {
        return VirtualMachine(
            id = id,
            name = name,
            distro = distro,
            region = region,
            ipAddress = ipAddress,
            publicIp = publicIp,
            isRunning = isRunning,
            cpuCores = cpuCores,
            ramMb = ramMb,
            diskGb = diskGb,
            cpuUsagePercent = if (isRunning) (8..35).random().toFloat() else 0f,
            ramUsageMb = if (isRunning) (ramMb * 0.22f).toInt() else 0,
            uptimeSeconds = if (isRunning) 5420L else 0L,
            installedPackageCount = 16
        )
    }

    private fun VirtualMachine.toEntity(): VirtualMachineEntity {
        return VirtualMachineEntity(
            id = id,
            name = name,
            distro = distro,
            region = region,
            ipAddress = ipAddress,
            publicIp = publicIp,
            isRunning = isRunning,
            cpuCores = cpuCores,
            ramMb = ramMb,
            diskGb = diskGb
        )
    }
}
